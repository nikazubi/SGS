package mthiebi.sgs.gradebook.service.journal;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.engine.ComponentDef;
import mthiebi.sgs.gradebook.engine.TemplateValidator;
import mthiebi.sgs.gradebook.engine.ValidationIssue;
import mthiebi.sgs.gradebook.engine.ValidationResult;
import mthiebi.sgs.gradebook.model.DerivationRule;
import mthiebi.sgs.gradebook.model.DerivationSource;
import mthiebi.sgs.gradebook.model.DerivationTerm;
import mthiebi.sgs.gradebook.model.GradeComponent;
import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.GradingTemplate;
import mthiebi.sgs.gradebook.model.TemplateAssignment;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.model.TemplateVersion;
import mthiebi.sgs.gradebook.model.TemplateVersionStatus;
import mthiebi.sgs.gradebook.repository.GradeComponentRepository;
import mthiebi.sgs.gradebook.repository.ClassGroupRepository;
import mthiebi.sgs.gradebook.repository.GradingTemplateRepository;
import mthiebi.sgs.gradebook.repository.TemplateVersionRepository;
import mthiebi.sgs.gradebook.service.TemplateGraphLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creating and editing journals.
 * <p>
 * A journal is a grid the school creates, names and sees in the menu. This
 * replaces TemplateScope, which was the three legacy journals written into an
 * enum - the school has run many over the years and changes them often, so a
 * journal is a row rather than a deployment.
 */
@Service
public class JournalService {


    @Autowired
    private GradingTemplateRepository journalRepository;

    @Autowired
    private TemplateVersionRepository versionRepository;

    @Autowired
    private GradeComponentRepository componentRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private TemplateGraphLoader templateGraphLoader;

    private final TemplateValidator validator = new TemplateValidator();

    @PersistenceContext
    private EntityManager em;

    // ---- the menu and the index -----------------------------------------

    @Transactional(readOnly = true)
    public List<JournalView> list(boolean includeArchived) {
        List<GradingTemplate> journals = includeArchived
                ? journalRepository.findAllOrdered()
                : journalRepository.findActive();
        return journals.stream().map(this::toView).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JournalView get(String uuid) throws SGSException {
        return toView(journal(uuid));
    }

    // ---- the wizard ------------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    public JournalView create(JournalDraft draft, Long periodSchemeId) throws SGSException {
        if (draft.getName() == null || draft.getName().trim().isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "ჟურნალს სახელი სჭირდება");
        }

        GradingTemplate journal = new GradingTemplate();
        journal.setName(draft.getName().trim());
        journal.setDescription(draft.getDescription());
        journal.setFrequency(draft.getFrequency());
        journal.setSubjectScoped(draft.isSubjectScoped());
        journal.setParentVisible(draft.isParentVisible());
        journal.setChartKey(emptyToNull(draft.getChartKey()));
        journal.setSortIndex(journalRepository.maxSortIndex() + 1);
        journalRepository.save(journal);

        TemplateVersion version = new TemplateVersion();
        version.setTemplate(journal);
        version.setVersionNo(1);
        // Draft until it has columns worth activating.
        version.setStatus(TemplateVersionStatus.DRAFT);
        version.setPeriodScheme(em.getReference(
                mthiebi.sgs.gradebook.model.PeriodScheme.class, periodSchemeId));
        versionRepository.save(version);

        return toView(journal);
    }

    @Transactional(rollbackFor = Exception.class)
    public JournalView update(String uuid, JournalDraft draft) throws SGSException {
        GradingTemplate journal = journal(uuid);
        if (draft.getName() != null && !draft.getName().trim().isEmpty()) {
            journal.setName(draft.getName().trim());
        }
        journal.setDescription(draft.getDescription());
        // These two are safe to change at any time: they affect who sees the
        // journal and how it is drawn, never what a stored cell means.
        journal.setParentVisible(draft.isParentVisible());
        journal.setChartKey(emptyToNull(draft.getChartKey()));
        // Frequency and shape are deliberately not editable once the journal
        // exists: both change what a stored cell means, and there is no
        // sensible reinterpretation of a trimester mark as a weekly one.
        return toView(journal);
    }

    @Transactional(rollbackFor = Exception.class)
    public void archive(String uuid, boolean archived) throws SGSException {
        // Never deleted. Grades point at a journal, so removing one would take
        // its history with it.
        journal(uuid).setArchived(archived);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reorder(List<String> uuidsInOrder) throws SGSException {
        int index = 0;
        for (String uuid : uuidsInOrder) {
            journal(uuid).setSortIndex(index++);
        }
    }

    // ---- the editor ------------------------------------------------------

    @Transactional(readOnly = true)
    public VersionStructure currentStructure(String uuid) throws SGSException {
        TemplateVersion version = editableVersionOf(journal(uuid));
        return structureOf(version);
    }

    /**
     * Save a whole version.
     * <p>
     * If any grade references the version, the save forks a draft instead of
     * editing in place: changing a live version would silently re-render marks
     * already entered under it, which is the failure the whole versioning
     * scheme exists to prevent.
     */
    @Transactional(rollbackFor = Exception.class)
    public SaveResult save(String uuid, Long versionId, List<ComponentDraft> components)
            throws SGSException {

        GradingTemplate journal = journal(uuid);
        TemplateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "ვერსია ვერ მოიძებნა"));

        requireBelongsTo(journal, version);

        boolean forked = false;
        if (!isEditableInPlace(version)) {
            version = fork(journal, version);
            forked = true;
        }

        applyComponents(version, components);
        em.flush();
        // Every journal that reads this one has its structure baked into a
        // cached graph, so evicting only this version would leave them
        // computing against the old shape until a restart.
        templateGraphLoader.evictAll();

        ValidationResult result = validate(version);

        // A draft may be saved broken - an editor has to be closable mid-thought
        // - but a version that is already live must never be. Decision 28 says a
        // teacher entering marks can never be shown a configuration error, and
        // an ACTIVE version with a cycle throws on every grid load.
        if (!result.isActivatable() && version.getStatus() == TemplateVersionStatus.ACTIVE) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "აქტიურ ჟურნალში შეცდომის შენახვა შეუძლებელია: "
                            + result.errors().get(0).getMessage());
        }
        return new SaveResult(structureOf(version), forked, result.issues(),
                result.isActivatable());
    }

    @Transactional(rollbackFor = Exception.class)
    public SaveResult activate(String uuid, Long versionId) throws SGSException {
        GradingTemplate journal = journal(uuid);
        TemplateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "ვერსია ვერ მოიძებნა"));
        requireBelongsTo(journal, version);

        ValidationResult result = validate(version);
        if (!result.isActivatable()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "ჟურნალში არის შეცდომები და ვერ გააქტიურდება");
        }

        // Existing periods stay pinned to whatever they were entered under.
        // Activation reaches future periods only; moving an existing one is a
        // separate, deliberate act with a recalculation attached.
        for (TemplateVersion other : versionRepository.findByTemplate(journal.getId())) {
            if (!other.getId().equals(version.getId())
                    && other.getStatus() == TemplateVersionStatus.ACTIVE) {
                other.setStatus(TemplateVersionStatus.ARCHIVED);
            }
        }
        version.setStatus(TemplateVersionStatus.ACTIVE);
        version.setActivatedAt(Instant.now());

        // Other journals move onto this version now, not when it was a draft.
        // Repointing on save sent them at components nobody writes to yet, and
        // an abandoned draft would have stranded them there.
        Map<String, GradeComponent> live = componentRepository
                .findByTemplateVersion(version.getId()).stream()
                .collect(Collectors.toMap(GradeComponent::getCode, c -> c,
                        (a, b) -> a, LinkedHashMap::new));
        repointExternalReferences(version, live);

        syncAssignments(journal, version);

        return new SaveResult(structureOf(version), false, result.issues(), true);
    }

    /**
     * Point every class at this version of the journal.
     * <p>
     * Without it a journal made in the wizard has no assignment, and the
     * resolver has nothing to fall back on for an empty period - so the grid
     * refuses to open, permanently. Re-pointing on activation is what makes
     * "future periods use the new version" true: the assignment names a
     * specific version, so leaving it alone would keep empty periods resolving
     * the superseded one.
     * <p>
     * Periods that already hold marks are untouched - they stay pinned to
     * whatever those marks were entered under, which is decision 24.
     */
    private void syncAssignments(GradingTemplate journal, TemplateVersion version) {
        List<ClassGroup> classes = classGroupRepository.findForCurrentYear();
        Map<Long, TemplateAssignment> existing = em.createQuery(
                        "select a from TemplateAssignment a "
                                + "where a.template.id = :t and a.subject is null",
                        TemplateAssignment.class)
                .setParameter("t", journal.getId())
                .getResultList().stream()
                .collect(Collectors.toMap(a -> a.getClassGroup().getId(), a -> a,
                        (a, b) -> a, HashMap::new));

        for (ClassGroup classGroup : classes) {
            TemplateAssignment assignment = existing.get(classGroup.getId());
            if (assignment == null) {
                assignment = new TemplateAssignment();
                assignment.setClassGroup(classGroup);
                assignment.setSubject(null);
                assignment.setTemplate(journal);
                // Set before persisting: templateVersion is NOT NULL, and
                // Hibernate checks it as the insert is prepared.
                assignment.setTemplateVersion(version);
                em.persist(assignment);
            } else {
                assignment.setTemplateVersion(version);
            }
        }
        em.flush();
    }

    // ---- the cross-journal picker ----------------------------------------

    @Transactional(readOnly = true)
    public List<ColumnRef> pickableColumns(String callerUuid) throws SGSException {
        GradingTemplate caller = callerUuid == null ? null : journal(callerUuid);
        return componentRepository.findAllLive().stream()
                .map(c -> {
                    GradingTemplate owner = c.getTemplateVersion().getTemplate();
                    return new ColumnRef(
                            owner.getUuid(), owner.getName(), owner.getFrequency(),
                            c.getCode(), c.getLabel(), c.getKind(),
                            caller != null && caller.getFrequency() == owner.getFrequency());
                })
                .collect(Collectors.toList());
    }

    // ---- internals -------------------------------------------------------

    /**
     * The version has to be this journal's.
     * <p>
     * Without it, activating journal A with a version id belonging to B points
     * A's every class at B's columns.
     */
    private void requireBelongsTo(GradingTemplate journal, TemplateVersion version)
            throws SGSException {
        if (!version.getTemplate().getId().equals(journal.getId())) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "ვერსია სხვა ჟურნალს ეკუთვნის");
        }
    }

    private GradingTemplate journal(String uuid) throws SGSException {
        return journalRepository.findByUuid(uuid)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "ჟურნალი ვერ მოიძებნა: " + uuid));
    }

    private boolean isEditableInPlace(TemplateVersion version) {
        if (version.getStatus() == TemplateVersionStatus.LOCKED) {
            return false;
        }
        Long used = em.createQuery(
                        "select count(g) from GradeEntry g where g.templateVersion.id = :v", Long.class)
                .setParameter("v", version.getId())
                .getSingleResult();
        return used == 0;
    }

    private TemplateVersion fork(GradingTemplate journal, TemplateVersion from) {
        int next = versionRepository.findByTemplate(journal.getId()).stream()
                .mapToInt(TemplateVersion::getVersionNo).max().orElse(0) + 1;

        TemplateVersion draft = new TemplateVersion();
        draft.setTemplate(journal);
        draft.setVersionNo(next);
        draft.setStatus(TemplateVersionStatus.DRAFT);
        draft.setPeriodScheme(from.getPeriodScheme());
        versionRepository.save(draft);
        em.flush();
        return draft;
    }

    /**
     * Diffs by code rather than replacing wholesale.
     * <p>
     * A column keeps its identity when it is renamed or moved, which matters
     * because another journal's formula may point at it - and because a
     * delete-and-recreate would orphan every such reference.
     */
    private void applyComponents(TemplateVersion version, List<ComponentDraft> drafts)
            throws SGSException {

        Map<String, GradeComponent> existing = componentRepository
                .findByTemplateVersion(version.getId()).stream()
                .collect(Collectors.toMap(GradeComponent::getCode, c -> c,
                        (a, b) -> a, LinkedHashMap::new));

        Map<String, GradeComponent> kept = new LinkedHashMap<>();
        for (ComponentDraft draft : drafts) {
            if (draft.getCode() == null || draft.getCode().trim().isEmpty()) {
                throw new SGSException(SGSExceptionCode.BAD_REQUEST, "სვეტს კოდი სჭირდება");
            }
            GradeComponent component = existing.get(draft.getCode());
            if (component == null) {
                component = new GradeComponent();
                component.setTemplateVersion(version);
                component.setCode(draft.getCode().trim());
            }
            component.setLabel(draft.getLabel());
            component.setOrdinal(draft.getOrdinal());
            component.setGroupLabel(emptyToNull(draft.getGroupLabel()));
            component.setKind(draft.getKind());
            component.setPeriodKind(draft.getPeriodKind());
            component.setSubjectScoped(draft.isSubjectScoped());
            component.setScaleMin(draft.getScaleMin());
            component.setScaleMax(draft.getScaleMax());
            component.setDecimals(draft.getDecimals());
            component.setAllowSpecialValues(draft.isAllowSpecialValues());
            component.setAllowOverride(draft.isAllowOverride());
            component.setParentVisible(draft.isParentVisible());
            em.persist(component);
            kept.put(component.getCode(), component);
        }

        for (Map.Entry<String, GradeComponent> gone : existing.entrySet()) {
            if (!kept.containsKey(gone.getKey())) {
                deleteRules(gone.getValue());
                em.remove(gone.getValue());
            }
        }
        em.flush();

        // Rules are rebuilt rather than diffed: nothing outside a component
        // references a term or a source, so their identity buys nothing.
        for (ComponentDraft draft : drafts) {
            GradeComponent component = kept.get(draft.getCode());
            deleteRules(component);
            if (draft.getRule() != null) {
                writeRule(component, draft.getRule(), kept);
            }
        }
        em.flush();
    }

    /**
     * Move other journals' formulas onto this version's columns.
     * <p>
     * A fork creates new component rows, so a formula elsewhere would keep
     * pointing at the superseded version - it would still validate, because
     * those components exist, but it would read cells nobody writes any more.
     * Matched by code, which is what a cross-journal reference means.
     */
    private void repointExternalReferences(TemplateVersion version,
                                           Map<String, GradeComponent> kept) {
        List<TemplateVersion> siblings =
                versionRepository.findByTemplate(version.getTemplate().getId());
        List<Long> supersededIds = new ArrayList<>();
        for (TemplateVersion sibling : siblings) {
            if (sibling.getId().equals(version.getId())) {
                continue;
            }
            componentRepository.findByTemplateVersion(sibling.getId())
                    .forEach(c -> supersededIds.add(c.getId()));
        }
        if (supersededIds.isEmpty()) {
            return;
        }

        // A LOCKED version's shape must not change: its grades have gone to
        // parents, and rewriting a formula inside it would silently alter what
        // they were shown.
        List<DerivationSource> external = em.createQuery(
                        "select s from DerivationSource s join fetch s.component c "
                                + "where c.id in :ids "
                                + "  and s.term.rule.component.templateVersion.template.id <> :journalId "
                                + "  and s.term.rule.component.templateVersion.status <> "
                                + "      mthiebi.sgs.gradebook.model.TemplateVersionStatus.LOCKED",
                        DerivationSource.class)
                .setParameter("ids", supersededIds)
                .setParameter("journalId", version.getTemplate().getId())
                .getResultList();

        for (DerivationSource source : external) {
            GradeComponent replacement = kept.get(source.getComponent().getCode());
            if (replacement != null) {
                source.setComponent(replacement);
            }
            // A column the new version dropped leaves its reference pointing at
            // the superseded component. Left silent it reads nothing forever,
            // so it is reported by validation rather than ignored.
        }
        em.flush();
    }

    private void deleteRules(GradeComponent component) {
        List<DerivationRule> rules = em.createQuery(
                        "select r from DerivationRule r where r.component.id = :c", DerivationRule.class)
                .setParameter("c", component.getId()).getResultList();
        for (DerivationRule rule : rules) {
            List<DerivationTerm> terms = em.createQuery(
                            "select t from DerivationTerm t where t.rule.id = :r", DerivationTerm.class)
                    .setParameter("r", rule.getId()).getResultList();
            for (DerivationTerm term : terms) {
                em.createQuery("delete from DerivationSource s where s.term.id = :t")
                        .setParameter("t", term.getId()).executeUpdate();
                em.remove(term);
            }
            em.remove(rule);
        }
        em.flush();
    }

    private void writeRule(GradeComponent component, RuleDraft draft,
                           Map<String, GradeComponent> siblings) throws SGSException {
        DerivationRule rule = new DerivationRule();
        rule.setComponent(component);
        rule.setChainOrder(0);
        rule.setType(draft.getType());
        rule.setNullPolicy(draft.getNullPolicy());
        rule.setRenormalizeWeights(draft.isRenormalizeWeights());
        rule.setRoundingMode(draft.getRoundingMode());
        rule.setDecimals(draft.getDecimals());
        em.persist(rule);

        int ordinal = 0;
        for (TermDraft termDraft : draft.getTerms()) {
            DerivationTerm term = new DerivationTerm();
            term.setRule(rule);
            term.setOrdinal(ordinal++);
            term.setWeight(termDraft.getWeight());
            term.setSourceKind(termDraft.getSourceKind());
            term.setReduce(termDraft.getReduce());
            term.setPeriodRef(termDraft.getPeriodRef());
            term.setPeriod(termDraft.getPeriodId() == null
                    ? null : em.getReference(Period.class, termDraft.getPeriodId()));
            term.setLabel(termDraft.getLabel());
            em.persist(term);

            for (SourceDraft sourceDraft : termDraft.getSources()) {
                DerivationSource source = new DerivationSource();
                source.setTerm(term);
                source.setComponent(resolveSource(sourceDraft, siblings));
                em.persist(source);
            }
        }
    }

    private GradeComponent resolveSource(SourceDraft draft, Map<String, GradeComponent> siblings)
            throws SGSException {
        if (draft.getJournalUuid() == null || draft.getJournalUuid().isEmpty()) {
            GradeComponent local = siblings.get(draft.getComponentCode());
            if (local == null) {
                throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "უცნობი სვეტი: " + draft.getComponentCode());
            }
            return local;
        }
        GradingTemplate other = journal(draft.getJournalUuid());
        TemplateVersion version = editableVersionOf(other);
        return componentRepository.findByTemplateVersion(version.getId()).stream()
                .filter(c -> c.getCode().equals(draft.getComponentCode()))
                .findFirst()
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "სვეტი " + draft.getComponentCode() + " ვერ მოიძებნა ჟურნალში "
                                + other.getName()));
    }

    /**
     * The version a new period would use: the active one, else the newest draft.
     */
    private TemplateVersion editableVersionOf(GradingTemplate journal) throws SGSException {
        List<TemplateVersion> versions = versionRepository.findByTemplate(journal.getId());
        return versions.stream()
                .filter(v -> v.getStatus() == TemplateVersionStatus.ACTIVE)
                .findFirst()
                .orElseGet(() -> versions.stream()
                        .max((a, b) -> Integer.compare(a.getVersionNo(), b.getVersionNo()))
                        .orElse(null));
    }

    private ValidationResult validate(TemplateVersion version) {
        // Across journals, not just this one: a reference into another journal
        // would otherwise look dangling, and a cycle running A -> B -> A would
        // never be seen.
        List<ComponentDef> reachable = templateGraphLoader.componentsReachableFrom(version.getId());
        return validator.validate(reachable);
    }

    private VersionStructure structureOf(TemplateVersion version) {
        VersionStructure structure = new VersionStructure();
        structure.setVersionId(version.getId());
        structure.setVersionNo(version.getVersionNo());
        structure.setStatus(version.getStatus());
        structure.setEditableInPlace(isEditableInPlace(version));

        Map<Long, String> journalByComponent = new HashMap<>();
        Map<Long, String> codeByComponent = new HashMap<>();

        List<GradeComponent> components = componentRepository.findByTemplateVersion(version.getId());
        for (GradeComponent c : components) {
            codeByComponent.put(c.getId(), c.getCode());
        }

        for (GradeComponent c : components) {
            ComponentDraft draft = new ComponentDraft();
            draft.setCode(c.getCode());
            draft.setLabel(c.getLabel());
            draft.setOrdinal(c.getOrdinal());
            draft.setGroupLabel(c.getGroupLabel());
            draft.setKind(c.getKind());
            draft.setPeriodKind(c.getPeriodKind());
            draft.setSubjectScoped(c.isSubjectScoped());
            draft.setScaleMin(c.getScaleMin());
            draft.setScaleMax(c.getScaleMax());
            draft.setDecimals(c.getDecimals());
            draft.setAllowSpecialValues(c.isAllowSpecialValues());
            draft.setAllowOverride(c.isAllowOverride());
            draft.setParentVisible(c.isParentVisible());
            draft.setRule(ruleDraftOf(c, codeByComponent, journalByComponent));
            structure.getComponents().add(draft);
        }
        return structure;
    }

    private RuleDraft ruleDraftOf(GradeComponent component, Map<Long, String> localCodes,
                                  Map<Long, String> journalByComponent) {
        List<DerivationRule> rules = em.createQuery(
                "select r from DerivationRule r where r.component.id = :c order by r.chainOrder",
                DerivationRule.class).setParameter("c", component.getId()).getResultList();
        if (rules.isEmpty()) {
            return null;
        }
        DerivationRule rule = rules.get(0);

        RuleDraft draft = new RuleDraft();
        draft.setType(rule.getType());
        draft.setNullPolicy(rule.getNullPolicy());
        draft.setRenormalizeWeights(rule.isRenormalizeWeights());
        draft.setRoundingMode(rule.getRoundingMode());
        draft.setDecimals(rule.getDecimals());

        List<DerivationTerm> terms = em.createQuery(
                "select t from DerivationTerm t where t.rule.id = :r order by t.ordinal",
                DerivationTerm.class).setParameter("r", rule.getId()).getResultList();

        for (DerivationTerm term : terms) {
            TermDraft termDraft = new TermDraft();
            termDraft.setWeight(term.getWeight());
            termDraft.setSourceKind(term.getSourceKind());
            termDraft.setReduce(term.getReduce());
            termDraft.setPeriodRef(term.getPeriodRef());
            termDraft.setPeriodId(term.getPeriod() == null ? null : term.getPeriod().getId());
            termDraft.setLabel(term.getLabel());

            List<DerivationSource> sources = em.createQuery(
                            "select s from DerivationSource s join fetch s.component c "
                                    + "join fetch c.templateVersion v join fetch v.template "
                                    + "where s.term.id = :t", DerivationSource.class)
                    .setParameter("t", term.getId()).getResultList();

            for (DerivationSource source : sources) {
                SourceDraft sourceDraft = new SourceDraft();
                GradeComponent target = source.getComponent();
                sourceDraft.setComponentCode(target.getCode());
                boolean local = localCodes.containsKey(target.getId());
                sourceDraft.setJournalUuid(local
                        ? null : target.getTemplateVersion().getTemplate().getUuid());
                termDraft.getSources().add(sourceDraft);
            }
            draft.getTerms().add(termDraft);
        }
        return draft;
    }

    private JournalView toView(GradingTemplate journal) {
        List<TemplateVersion> versions = versionRepository.findByTemplate(journal.getId());
        TemplateVersion current = versions.stream()
                .filter(v -> v.getStatus() == TemplateVersionStatus.ACTIVE)
                .findFirst()
                .orElseGet(() -> versions.stream()
                        .max((a, b) -> Integer.compare(a.getVersionNo(), b.getVersionNo()))
                        .orElse(null));

        int columns = current == null ? 0
                : componentRepository.findByTemplateVersion(current.getId()).size();

        return new JournalView(journal.getUuid(), journal.getName(), journal.getDescription(),
                journal.getFrequency(), journal.isSubjectScoped(), journal.getSortIndex(),
                journal.isArchived(),
                journal.isParentVisible(),
                journal.getChartKey(),
                journal.getGridMode() == null ? null : journal.getGridMode().name(),
                current == null ? null : current.getId(),
                current == null ? 0 : current.getVersionNo(),
                columns,
                current == null ? null : current.getStatus().name());
    }

    private String emptyToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
