package mthiebi.sgs.gradebook.service;

import mthiebi.sgs.gradebook.engine.ComponentDef;
import mthiebi.sgs.gradebook.engine.RuleDef;
import mthiebi.sgs.gradebook.engine.TemplateGraph;
import mthiebi.sgs.gradebook.engine.TermDef;
import mthiebi.sgs.gradebook.model.DerivationRule;
import mthiebi.sgs.gradebook.model.DerivationSource;
import mthiebi.sgs.gradebook.model.DerivationTerm;
import mthiebi.sgs.gradebook.model.GradeComponent;
import mthiebi.sgs.gradebook.repository.DerivationRuleRepository;
import mthiebi.sgs.gradebook.repository.DerivationSourceRepository;
import mthiebi.sgs.gradebook.repository.DerivationTermRepository;
import mthiebi.sgs.gradebook.repository.GradeComponentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Turns a stored template version into the engine's executable form.
 * <p>
 * Cached, because a version's shape is immutable once activated - that is the
 * point of versioning it. Editing a draft evicts it; an active version is never
 * rewritten, only superseded.
 * <p>
 * Four small queries rather than a graph walk over lazy associations: the whole
 * template is a few dozen rows, and loading it flat keeps this off the hot path
 * of a grade save entirely.
 */
@Service
public class TemplateGraphLoader {

    private final Map<Long, TemplateGraph> cache = new ConcurrentHashMap<>();

    @Autowired
    private GradeComponentRepository componentRepository;

    @javax.persistence.PersistenceContext
    private javax.persistence.EntityManager em;

    @Autowired
    private DerivationRuleRepository ruleRepository;

    @Autowired
    private DerivationTermRepository termRepository;

    @Autowired
    private DerivationSourceRepository sourceRepository;

    public TemplateGraph graphOf(Long templateVersionId) {
        return cache.computeIfAbsent(templateVersionId,
                id -> TemplateGraph.build(componentsReachableFrom(id)));
    }

    /**
     * This version's components plus every version its formulas reach into.
     * <p>
     * A column may read a column in another journal, so a graph built from one
     * version alone would see those references as dangling and would never
     * notice a cycle that runs A -> B -> A. Following them also gives the
     * recompute engine its cross-journal fan-out for free: saving an ethics
     * mark finds the academic column that depends on it, because the dependency
     * edge is in the same graph.
     * <p>
     * Journals reference each other rarely and shallowly, so this closes over a
     * handful of versions rather than the whole school.
     * <p>
     * Only ONE version of any journal may take part. A journal has many
     * versions and forks copy their column codes, so admitting two would make
     * duplicate codes by construction - and TemplateGraph rejects duplicates,
     * which would leave every grid of the referenced journal throwing. The
     * version in force is the one a reader is entitled to see: drafts are not
     * live and archived ones are no longer live.
     */
    @Transactional(readOnly = true)
    public List<ComponentDef> componentsReachableFrom(Long templateVersionId) {
        Map<Long, ComponentDef> byId = new LinkedHashMap<>();
        Set<Long> visitedVersions = new HashSet<>();
        Set<Long> visitedJournals = new HashSet<>();
        Deque<Long> pending = new ArrayDeque<>();
        pending.add(templateVersionId);

        while (!pending.isEmpty()) {
            Long versionId = pending.poll();
            if (!visitedVersions.add(versionId)) {
                continue;
            }
            if (!visitedJournals.add(journalOf(versionId))) {
                // Another version of this journal is already in the closure.
                continue;
            }
            List<ComponentDef> defs = componentsOf(versionId);
            defs.forEach(def -> byId.putIfAbsent(def.getId(), def));

            Set<Long> unresolved = new LinkedHashSet<>();
            for (ComponentDef def : defs) {
                if (def.getRule() == null) {
                    continue;
                }
                for (Long sourceId : def.getRule().allSourceComponentIds()) {
                    if (!byId.containsKey(sourceId)) {
                        unresolved.add(sourceId);
                    }
                }
            }

            // Both directions. Following only what this version reads walks
            // upstream, so a journal that reads *us* is never in the graph -
            // its dependency edge would not exist, and the recompute engine
            // would leave its cells stale after every edit here.
            List<Long> ownIds = defs.stream().map(ComponentDef::getId)
                    .collect(Collectors.toList());
            if (!ownIds.isEmpty()) {
                for (Long readerId : sourceRepository.findComponentIdsReading(ownIds)) {
                    if (!byId.containsKey(readerId)) {
                        unresolved.add(readerId);
                    }
                }
            }

            if (!unresolved.isEmpty()) {
                pending.addAll(componentRepository.findVersionIdsOf(unresolved));
            }
        }
        return new ArrayList<>(byId.values());
    }

    private Long journalOf(Long templateVersionId) {
        List<Long> found = em.createQuery(
                        "select v.template.id from TemplateVersion v where v.id = :id", Long.class)
                .setParameter("id", templateVersionId).getResultList();
        return found.isEmpty() ? templateVersionId : found.get(0);
    }

    /**
     * Which version each component belongs to, for writing cells back.
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> versionIdByComponent(Long templateVersionId) {
        Map<Long, Long> map = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        Deque<Long> pending = new ArrayDeque<>();
        pending.add(templateVersionId);
        while (!pending.isEmpty()) {
            Long versionId = pending.poll();
            if (!visited.add(versionId)) {
                continue;
            }
            List<ComponentDef> defs = componentsOf(versionId);
            Set<Long> unresolved = new LinkedHashSet<>();
            List<Long> ownIds = new ArrayList<>();
            for (ComponentDef def : defs) {
                map.put(def.getId(), versionId);
                ownIds.add(def.getId());
                if (def.getRule() != null) {
                    def.getRule().allSourceComponentIds().forEach(id -> {
                        if (!map.containsKey(id)) {
                            unresolved.add(id);
                        }
                    });
                }
            }
            if (!ownIds.isEmpty()) {
                for (Long readerId : sourceRepository.findComponentIdsReading(ownIds)) {
                    if (!map.containsKey(readerId)) {
                        unresolved.add(readerId);
                    }
                }
            }
            if (!unresolved.isEmpty()) {
                pending.addAll(componentRepository.findVersionIdsOf(unresolved));
            }
        }
        return map;
    }

    /**
     * Call when a draft version is edited. Active versions never need this.
     */
    public void evict(Long templateVersionId) {
        cache.remove(templateVersionId);
    }

    public void evictAll() {
        cache.clear();
    }

    @Transactional(readOnly = true)
    public List<ComponentDef> componentsOf(Long templateVersionId) {
        List<GradeComponent> components = componentRepository.findByTemplateVersion(templateVersionId);
        if (components.isEmpty()) {
            return Collections.emptyList();
        }

        List<DerivationRule> rules = ruleRepository.findByTemplateVersion(templateVersionId);
        Map<Long, List<TermDef>> termsByRule = termsByRule(rules);

        // Rules arrive grouped by component and already in chain order.
        Map<Long, List<DerivationRule>> chains = new LinkedHashMap<>();
        for (DerivationRule rule : rules) {
            chains.computeIfAbsent(rule.getComponent().getId(), k -> new ArrayList<>()).add(rule);
        }

        List<ComponentDef> defs = new ArrayList<>(components.size());
        for (GradeComponent component : components) {
            RuleDef rule = ruleChain(chains.get(component.getId()), termsByRule);
            defs.add(new ComponentDef(
                    component.getId(),
                    component.getCode(),
                    component.getLabel(),
                    component.getOrdinal(),
                    component.getKind(),
                    component.getPeriodKind(),
                    component.isSubjectScoped(),
                    component.isAllowOverride(),
                    component.getDecimals(),
                    component.getScaleMin(),
                    component.getScaleMax(),
                    component.isAllowSpecialValues(),
                    rule));
        }
        return defs;
    }

    private Map<Long, List<TermDef>> termsByRule(List<DerivationRule> rules) {
        if (rules.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ruleIds = rules.stream().map(DerivationRule::getId).collect(Collectors.toList());
        List<DerivationTerm> terms = termRepository.findByRuleIds(ruleIds);
        if (terms.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> termIds = terms.stream().map(DerivationTerm::getId).collect(Collectors.toList());
        Map<Long, List<Long>> sourcesByTerm = sourceRepository.findByTermIds(termIds).stream()
                .collect(Collectors.groupingBy(
                        s -> s.getTerm().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(s -> s.getComponent().getId(), Collectors.toList())));

        Map<Long, List<TermDef>> byRule = new LinkedHashMap<>();
        for (DerivationTerm term : terms) {
            TermDef def = new TermDef(
                    term.getOrdinal(),
                    term.getWeight(),
                    term.getSourceKind(),
                    term.getReduce(),
                    term.getPeriodRef(),
                    term.getPeriod() == null ? null : term.getPeriod().getId(),
                    sourcesByTerm.getOrDefault(term.getId(), Collections.emptyList()),
                    term.getLabel());
            byRule.computeIfAbsent(term.getRule().getId(), k -> new ArrayList<>()).add(def);
        }
        return byRule;
    }

    /**
     * Builds the chain back to front so each rule points at its successor.
     */
    private RuleDef ruleChain(List<DerivationRule> chain, Map<Long, List<TermDef>> termsByRule) {
        if (chain == null || chain.isEmpty()) {
            return null;
        }
        RuleDef next = null;
        for (int i = chain.size() - 1; i >= 0; i--) {
            DerivationRule rule = chain.get(i);
            next = new RuleDef(
                    rule.getType(),
                    rule.getNullPolicy(),
                    rule.isRenormalizeWeights(),
                    rule.getRoundingMode(),
                    rule.getDecimals(),
                    termsByRule.getOrDefault(rule.getId(), Collections.emptyList()),
                    next);
        }
        return next;
    }
}
