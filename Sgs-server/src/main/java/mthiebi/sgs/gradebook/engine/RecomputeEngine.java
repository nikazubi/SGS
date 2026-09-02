package mthiebi.sgs.gradebook.engine;

import mthiebi.sgs.gradebook.model.PeriodRef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Propagates a change through a template's derived columns.
 * <p>
 * Fan-out runs along three independent axes, and getting only the first is the
 * easy mistake:
 * <p>
 * component - ONGOING_3 -> ONGOING_AVG -> TRIMESTER_GRADE
 * period    - T1.TRIMESTER_GRADE -> YEAR.ANNUAL   (CHILDREN terms)
 * subject   - any subject's grade -> the subject-less RATING (ALL_SUBJECTS)
 * <p>
 * Because of the period axis a plain topological sort over components is not
 * enough: YEAR.ANNUAL depends on the same component at a deeper period. Cells
 * are therefore ordered by period depth descending first, so leaves resolve
 * before the rollups that consume them.
 */
public class RecomputeEngine {

    private final Evaluator evaluator;

    public RecomputeEngine() {
        this(new Evaluator());
    }

    public RecomputeEngine(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    /**
     * @param seeds cells whose values have just been written by a person
     * @return the derived cells actually written, in evaluation order
     */
    public List<CellKey> recompute(Collection<CellKey> seeds, EvaluationContext ctx) {
        return recompute(seeds, Collections.emptyList(), ctx);
    }

    /**
     * @param alsoEvaluate cells to recompute even though nothing downstream of
     *                     them changed - used when an override is cleared and
     *                     the cell must re-derive itself
     */
    public List<CellKey> recompute(Collection<CellKey> seeds,
                                   Collection<CellKey> alsoEvaluate,
                                   EvaluationContext ctx) {

        Set<CellKey> affected = new LinkedHashSet<>(alsoEvaluate);
        Deque<CellKey> frontier = new ArrayDeque<>(seeds);
        frontier.addAll(alsoEvaluate);

        while (!frontier.isEmpty()) {
            CellKey current = frontier.poll();
            for (Long dependentId : ctx.getGraph().dependentsOf(current.getComponentId())) {
                ComponentDef dependent = ctx.getGraph().byId(dependentId);
                for (CellKey target : targetsOf(current, dependent, ctx)) {
                    if (affected.add(target)) {
                        frontier.add(target);
                    }
                }
            }
        }

        List<CellKey> ordered = new ArrayList<>(affected);
        ordered.sort(orderingFor(ctx));

        List<CellKey> written = new ArrayList<>();
        for (CellKey cell : ordered) {
            ComponentDef component = ctx.getGraph().byId(cell.getComponentId());
            if (!component.isDerived()) {
                continue;
            }

            // An overridden cell keeps the value a person typed, but still feeds
            // everything downstream - it behaves as an input from here on.
            Optional<CellValue> existing = ctx.getWorkingSet().get(cell);
            if (existing.isPresent() && existing.get().isOverride()) {
                continue;
            }

            EvalOutcome outcome = evaluator.evaluate(cell, component, ctx);
            if (ctx.getWorkingSet().put(cell, CellValue.derived(outcome.getValue()))) {
                written.add(cell);
            }
        }
        return written;
    }

    /**
     * Which cells of {@code dependent} read the cell that just changed.
     */
    private Set<CellKey> targetsOf(CellKey changed, ComponentDef dependent, EvaluationContext ctx) {
        RuleDef rule = dependent.getRule();
        if (rule == null) {
            return Collections.emptySet();
        }

        Set<Long> periods = new LinkedHashSet<>();
        RuleDef current = rule;
        while (current != null) {
            for (TermDef term : current.getTerms()) {
                if (!term.getSourceComponentIds().contains(changed.getComponentId())) {
                    continue;
                }
                periods.addAll(periodsReading(term, changed, dependent, ctx));
            }
            current = current.getFallback();
        }

        // Which of the dependent's cells are affected depends on both shapes.
        // A class-wide dependent has one cell per period whatever moved. A
        // per-subject dependent normally has the one matching cell - but when
        // the change was to a class-wide column, every subject's cell reads it,
        // and copying the null through would build a subject-null key for a
        // subject-scoped component and persist a corrupt row.
        List<Long> subjects;
        if (!dependent.isSubjectScoped()) {
            subjects = Collections.singletonList(null);
        } else if (changed.getSubjectId() != null) {
            subjects = Collections.singletonList(changed.getSubjectId());
        } else {
            subjects = ctx.getSubjectIds();
        }

        Set<CellKey> targets = new LinkedHashSet<>();
        for (Long periodId : periods) {
            if (periodId == null) {
                continue;
            }
            for (Long subjectId : subjects) {
                targets.add(new CellKey(changed.getEnrollmentId(), subjectId, periodId,
                        dependent.getId()));
            }
        }
        return targets;
    }

    /**
     * Which level of the tree the dependent's own cells sit at.
     * <p>
     * The component says which kind of period it lives on and the tree says
     * where that kind sits. This used to be a binary - the root for a YEAR
     * rollup, the journal's own level for everything else - which made a
     * trimester column on a monthly journal impossible to express, and the
     * brief asks for exactly that in three of its four tables.
     * <p>
     * Falls back to the journal's own level when the scheme has no period of
     * the kind: a template can name a tier its scheme does not have, and
     * recomputing at the journal's level is the old behaviour rather than none.
     */
    private int dependentDepth(ComponentDef dependent, EvaluationContext ctx) {
        int depth = ctx.getPeriodTree().depthOfKind(dependent.getPeriodKind());
        return depth < 0 ? ctx.getJournalDepth() : depth;
    }

    /**
     * Which periods hold cells of the dependent that read the changed one.
     * <p>
     * The inverse of what the evaluator resolves, and the half that has been
     * wrong most often - "one level up" is the inverse of CHILDREN alone, and
     * applying it to DESCENDANTS persisted a yearly total onto a month. Both
     * directions now come from {@link PeriodReach}, so they cannot drift apart.
     */
    private List<Long> periodsReading(TermDef term, CellKey changed,
                                      ComponentDef dependent, EvaluationContext ctx) {
        PeriodReach reach = PeriodReach.of(ctx.getPeriodTree());
        List<Long> periods = reach.dependents(
                term.getPeriodRef(), changed.getPeriodId(), dependentDepth(dependent, ctx),
                term.getSpecificPeriodId());

        // A column lives on one kind of period, so any other period the shape
        // resolves to is a row nothing would ever read. That is what db/023 had
        // to clean up: a YEAR rollup written with a CHILDREN term resolved its
        // dependent as the *parent* of the changed cell - the trimester - and
        // the write path duly stored the year's total there.
        //
        // Filtered on the component's own kind rather than on a depth number,
        // because the journal depth is set by the caller and is not always
        // present; filtering on an absent value would recompute nothing at all.
        // This was a YEAR-only guard; it is the general form of the same rule,
        // and it is what keeps a trimester rollup off the reporting periods.
        return periods.stream()
                .filter(id -> ctx.getPeriodTree().node(id).getKind()
                        == dependent.getPeriodKind())
                .collect(java.util.stream.Collectors.toList());
    }

    private Comparator<CellKey> orderingFor(EvaluationContext ctx) {
        return Comparator
                .comparingInt((CellKey c) -> -ctx.getPeriodTree().depth(c.getPeriodId()))
                .thenComparingInt(c -> ctx.getGraph().topoIndexOf(c.getComponentId()));
    }
}
