package mthiebi.sgs.gradebook.engine;

import mthiebi.sgs.gradebook.model.PeriodRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Which periods a rule touches.
 * <p>
 * Every question of the form "given this period, which others are involved?"
 * lives here and nowhere else. That is the point of the class: the six services
 * that ask it were each answering it themselves, in six inline expressions that
 * had to agree and were never checked against one another.
 * <p>
 * The cost of that was not theoretical. {@link PeriodRef#DESCENDANTS} was added
 * for the absence rollups and taught to four of the six. The working set kept
 * its old neighbourhood, so a yearly total was computed from one month of data
 * and written over the year; the explain trace still resolves different sources
 * than the write path used. Each copy had passing tests. Nothing asserted they
 * agreed, because there was nothing to assert about.
 * <p>
 * There are five questions here, and they are genuinely different:
 *
 * <ul>
 *   <li>{@link #sources} - evaluating a cell, where its inputs are read from.</li>
 *   <li>{@link #dependents} - a cell changed; which periods hold rollups that
 *       read it. The inverse of the first, and the one that has been wrong most
 *       often, because "one level up" is only the inverse of "one level down".</li>
 *   <li>{@link #workingSet} - everything to load before evaluating, so that no
 *       rule sees a fraction of its inputs and stores the result anyway.</li>
 *   <li>{@link #subtree} - a period and everything beneath it, for publication.</li>
 *   <li>{@link #atDepth} - one level beneath a period, for a transposed grid.</li>
 * </ul>
 * <p>
 * Cheap to construct; the tree it wraps is the cached one.
 */
public final class PeriodReach {

    private final PeriodTree tree;

    private PeriodReach(PeriodTree tree) {
        this.tree = tree;
    }

    public static PeriodReach of(PeriodTree tree) {
        return new PeriodReach(tree);
    }

    public PeriodTree tree() {
        return tree;
    }

    /**
     * Evaluating a cell: the periods a term reads its sources from.
     *
     * @param journalDepth the level the source column is filled in at. Consulted
     *                     only for DESCENDANTS, the one ref whose reach is not a
     *                     fixed number of hops.
     */
    public List<Long> sources(PeriodRef ref, Long periodId, int journalDepth, Long specificId) {
        if (ref == PeriodRef.CHILDREN) {
            return tree.children(periodId);
        }
        if (ref == PeriodRef.DESCENDANTS) {
            // The sources may be several levels below the cell being computed:
            // a yearly absence total reads months two down, past trimesters
            // that hold nothing at all.
            return tree.descendantsAtDepth(periodId, journalDepth);
        }
        if (ref == PeriodRef.SPECIFIC) {
            return specificId == null ? Collections.emptyList()
                    : Collections.singletonList(specificId);
        }
        return Collections.singletonList(periodId);
    }

    /**
     * A cell changed: the periods whose cells read it, for a term of this shape.
     * <p>
     * The inverse of {@link #sources}, and where the subtlety is. CHILDREN reads
     * one level down, so its inverse is one level up - the parent. DESCENDANTS
     * reads any distance down, so its inverse is not the parent: it is whichever
     * ancestor sits at the level the dependent column lives at. A monthly mark
     * feeding a yearly total has to recompute the year, and resolving it as the
     * parent wrote the year's total onto the trimester instead, a level that
     * column does not exist at.
     *
     * @param dependentDepth the level the dependent column lives at: 0 for a
     *                       YEAR rollup, otherwise the journal's own.
     */
    public List<Long> dependents(PeriodRef ref, Long changedPeriodId, int dependentDepth,
                                 Long specificId) {
        if (ref == PeriodRef.SAME) {
            return Collections.singletonList(changedPeriodId);
        }
        if (ref == PeriodRef.CHILDREN) {
            Long parent = tree.parent(changedPeriodId);
            return parent == null ? Collections.emptyList() : Collections.singletonList(parent);
        }
        if (ref == PeriodRef.DESCENDANTS) {
            for (Long ancestor : tree.ancestors(changedPeriodId)) {
                if (tree.depth(ancestor) == dependentDepth) {
                    return Collections.singletonList(ancestor);
                }
            }
            return Collections.emptyList();
        }
        // SPECIFIC pins the source to one named period, so any cell of the
        // dependent may read it - bounded by the levels the dependent lives at,
        // not by the changed period's, which is the entire point of the ref: it
        // exists so a column can read one at a different frequency.
        if (!changedPeriodId.equals(specificId)) {
            return Collections.emptyList();
        }
        return tree.periodsAtDepth(dependentDepth);
    }

    /**
     * Keeps only the periods a YEAR column can actually live at - the root.
     * <p>
     * Used by the recompute engine, which knows the component's kind. It is not
     * applied inside {@link #dependents} because the depth passed there is the
     * journal's, which a caller may not have set, and filtering on a number that
     * is sometimes absent is worse than not filtering: it silently recomputes
     * nothing.
     */
    public List<Long> onlyAtRoot(List<Long> periodIds) {
        List<Long> result = new ArrayList<>();
        for (Long periodId : periodIds) {
            if (periodId != null && tree.depth(periodId) == 0) {
                result.add(periodId);
            }
        }
        return result;
    }

    /**
     * Everything that must be in memory before a write to this period is
     * evaluated.
     * <p>
     * A rule evaluated against a working set missing some of its sources does
     * not fail - it computes a value from what is there and stores it. So the
     * set has to cover every period any rule in the graph can reach, and the
     * graph is inspected rather than assumed.
     * <p>
     * The neighbourhood - self, ancestors, children, each ancestor's children -
     * covers SAME and CHILDREN. DESCENDANTS needs the full subtree of self and
     * of every ancestor, and is paid for only by a graph that uses it. SPECIFIC
     * names periods anywhere in the scheme, so they are collected by name.
     */
    public List<Long> workingSet(Long periodId, TemplateGraph graph) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(periodId);
        ids.addAll(tree.ancestors(periodId));
        ids.addAll(tree.children(periodId));
        for (Long ancestor : tree.ancestors(periodId)) {
            ids.addAll(tree.children(ancestor));
        }

        if (usesDescendants(graph)) {
            List<Long> roots = new ArrayList<>(tree.ancestors(periodId));
            roots.add(periodId);
            for (Long root : roots) {
                ids.addAll(subtree(root));
            }
        }

        ids.addAll(specificPeriods(graph));
        return new ArrayList<>(ids);
    }

    /**
     * A period and everything beneath it, however deep.
     * <p>
     * What publication releases. Walked rather than joined on parent_id because
     * the gap is not always one level - publishing a year has to reach months
     * two levels down, with trimesters in between.
     */
    public List<Long> subtree(Long periodId) {
        List<Long> all = new ArrayList<>();
        all.add(periodId);
        for (int depth = tree.depth(periodId) + 1; depth <= tree.maxDepth(); depth++) {
            all.addAll(tree.descendantsAtDepth(periodId, depth));
        }
        return all;
    }

    /**
     * Everything beneath a period at exactly one level - the columns of a transposed grid.
     */
    public List<Long> atDepth(Long ancestorId, int depth) {
        if (tree.depth(ancestorId) >= depth) {
            return Collections.emptyList();
        }
        return tree.descendantsAtDepth(ancestorId, depth);
    }

    /**
     * Every period a SPECIFIC term names, anywhere in the scheme.
     */
    public Set<Long> specificPeriods(TemplateGraph graph) {
        Set<Long> ids = new LinkedHashSet<>();
        for (ComponentDef component : graph.all()) {
            RuleDef rule = component.getRule();
            while (rule != null) {
                for (TermDef term : rule.getTerms()) {
                    if (term.getPeriodRef() == PeriodRef.SPECIFIC
                            && term.getSpecificPeriodId() != null) {
                        ids.add(term.getSpecificPeriodId());
                    }
                }
                rule = rule.getFallback();
            }
        }
        return ids;
    }

    /**
     * Whether any rule in the graph reaches further than one level down.
     */
    public static boolean usesDescendants(TemplateGraph graph) {
        for (ComponentDef component : graph.all()) {
            RuleDef rule = component.getRule();
            while (rule != null) {
                for (TermDef term : rule.getTerms()) {
                    if (term.getPeriodRef() == PeriodRef.DESCENDANTS) {
                        return true;
                    }
                }
                rule = rule.getFallback();
            }
        }
        return false;
    }
}
