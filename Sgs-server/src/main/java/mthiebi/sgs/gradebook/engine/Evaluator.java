package mthiebi.sgs.gradebook.engine;

import mthiebi.sgs.gradebook.model.NullPolicy;
import mthiebi.sgs.gradebook.model.PeriodRef;
import mthiebi.sgs.gradebook.model.ReduceType;
import mthiebi.sgs.gradebook.model.RuleType;
import mthiebi.sgs.gradebook.model.SourceKind;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Computes one derived cell from its rule.
 * <p>
 * Pure: it reads the working set and the template graph and touches nothing
 * else, which is what makes it unit-testable without a database and why the
 * rules from the legacy system can be pinned down in tests rather than
 * rediscovered from behaviour.
 * <p>
 * Two invariants worth stating up front:
 * <p>
 * - Rounding happens once, on the final value. Intermediates are carried at
 * full precision. The old code rounded at each step and inconsistently -
 * Math.round in some paths, HALF_UP at varying scales in others - which
 * quietly moved marks by a point.
 * <p>
 * - Absent input is normal and yields null. It is never silently zero unless
 * a rule explicitly says AS_ZERO. Conflating the two is how zeros appear in
 * report cards for work a teacher simply had not entered yet.
 */
public class Evaluator {

    /**
     * Enough precision that intermediate division does not throw or truncate.
     */
    private static final MathContext MC = MathContext.DECIMAL64;

    public EvalOutcome evaluate(CellKey cell, ComponentDef component, EvaluationContext ctx) {
        return evaluate(cell, component, ctx, false);
    }

    public EvalOutcome evaluate(CellKey cell, ComponentDef component, EvaluationContext ctx, boolean trace) {
        if (!component.isDerived()) {
            return EvalOutcome.empty();
        }

        RuleDef rule = component.getRule();
        boolean usedFallback = false;

        while (rule != null) {
            RuleOutcome outcome = applyRule(rule, cell, ctx, trace);
            if (outcome.value != null) {
                BigDecimal rounded = outcome.value.setScale(rule.getDecimals(), rule.getRoundingMode());
                EvaluationTrace t = trace
                        ? new EvaluationTrace(component.getCode(), rule.getType(), outcome.termTraces,
                        usedFallback, outcome.value, rounded,
                        rule.getRoundingMode(), rule.getDecimals())
                        : null;
                return new EvalOutcome(rounded, t);
            }
            // Nothing produced: try the next rule in the chain. This is how the
            // legacy "avg(S1,S2), else avg(one, resit), else resit alone" rule
            // is expressed without any conditional logic in code.
            rule = rule.getFallback();
            usedFallback = true;
        }

        return EvalOutcome.empty();
    }

    // ---------------------------------------------------------------- rules

    private RuleOutcome applyRule(RuleDef rule, CellKey cell, EvaluationContext ctx, boolean trace) {
        List<BigDecimal> termValues = new ArrayList<>();
        List<BigDecimal> termWeights = new ArrayList<>();
        List<TermTrace> termTraces = trace ? new ArrayList<>() : Collections.emptyList();

        for (TermDef term : rule.getTerms()) {
            List<ResolvedSource> resolved = resolveSources(term, cell, ctx);
            ReduceOutcome reduced = reduceTerm(term, resolved, rule.getNullPolicy(), ctx, trace);

            if (reduced.blocked) {
                // BLOCK means this rule cannot produce a value at all; the
                // fallback chain may still offer one.
                return RuleOutcome.none(termTraces);
            }

            termValues.add(reduced.value);
            termWeights.add(term.getWeight() == null ? BigDecimal.ONE : term.getWeight());

            if (trace) {
                termTraces.add(new TermTrace(term.getLabel(), term.getWeight(),
                        reduced.value, reduced.traces));
            }
        }

        BigDecimal value = aggregate(rule, termValues, termWeights);
        return new RuleOutcome(value, termTraces);
    }

    private BigDecimal aggregate(RuleDef rule, List<BigDecimal> values, List<BigDecimal> weights) {
        List<BigDecimal> present = new ArrayList<>();
        for (BigDecimal v : values) {
            if (v != null) {
                present.add(v);
            }
        }
        if (present.isEmpty()) {
            return null;
        }

        RuleType type = rule.getType();
        switch (type) {
            case WEIGHTED_SUM:
                return weightedSum(rule, values, weights);
            case SUM:
                return present.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            case AVERAGE:
                return present.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(present.size()), MC);
            case MIN:
                return Collections.min(present);
            case MAX:
                return Collections.max(present);
            case FIRST_NON_NULL:
                return present.get(0);
            default:
                throw new TemplateGraphException("unsupported rule type: " + type);
        }
    }

    private BigDecimal weightedSum(RuleDef rule, List<BigDecimal> values, List<BigDecimal> weights) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal declaredWeight = BigDecimal.ZERO;
        BigDecimal survivingWeight = BigDecimal.ZERO;

        for (int i = 0; i < values.size(); i++) {
            BigDecimal weight = weights.get(i);
            declaredWeight = declaredWeight.add(weight);
            BigDecimal value = values.get(i);
            if (value != null) {
                total = total.add(value.multiply(weight, MC), MC);
                survivingWeight = survivingWeight.add(weight);
            }
        }

        if (survivingWeight.signum() == 0) {
            return null;
        }

        // Without this, a student missing the 30% final test is quietly capped
        // at 70% of scale rather than judged on the work they did do.
        if (rule.isRenormalizeWeights() && survivingWeight.compareTo(declaredWeight) != 0) {
            total = total.multiply(declaredWeight, MC).divide(survivingWeight, MC);
        }
        return total;
    }

    // -------------------------------------------------------------- sources

    private List<ResolvedSource> resolveSources(TermDef term, CellKey cell, EvaluationContext ctx) {
        List<Long> periods = targetPeriods(term, cell, ctx);

        List<ResolvedSource> resolved = new ArrayList<>();
        for (Long componentId : term.getSourceComponentIds()) {
            // Resolved per source, not per term: whether a cell is held per
            // subject is a property of the column being read, and a term may
            // read columns of both shapes - a per-subject column reading a
            // class-wide one would otherwise look for a subject-keyed cell
            // that is stored subject-null, and miss it forever.
            List<Long> subjects = targetSubjects(term, componentId, cell, ctx);
            for (Long periodId : periods) {
                for (Long subjectId : subjects) {
                    CellKey key = new CellKey(cell.getEnrollmentId(), subjectId, periodId, componentId);
                    resolved.add(new ResolvedSource(componentId, periodId, subjectId,
                            ctx.getWorkingSet().get(key)));
                }
            }
        }
        return resolved;
    }

    /**
     * Where this term reads its sources from.
     * <p>
     * Delegated to {@link PeriodReach}, which is the only place that answers it.
     * The recompute engine asks the inverse question and the write path asks
     * what to load; when those three were each written out here and there, they
     * disagreed, and a disagreement between them is not a failure - it is a
     * total computed from part of its inputs and stored as if complete.
     */
    private List<Long> targetPeriods(TermDef term, CellKey cell, EvaluationContext ctx) {
        return PeriodReach.of(ctx.getPeriodTree()).sources(
                term.getPeriodRef(), cell.getPeriodId(), ctx.getJournalDepth(),
                term.getSpecificPeriodId());
    }

    /**
     * Which subjects a source is read under.
     * <p>
     * ALL_SUBJECTS spans every subject by definition. Otherwise the source
     * column's own shape decides: a class-wide column has exactly one cell per
     * period and is stored subject-null, while a per-subject column read from a
     * class-wide dependent has one cell per subject and they are all read.
     */
    private List<Long> targetSubjects(TermDef term, Long sourceComponentId, CellKey cell,
                                      EvaluationContext ctx) {
        if (term.getSourceKind() == SourceKind.ALL_SUBJECTS) {
            ComponentDef allSubjectsSource = ctx.getGraph().byId(sourceComponentId);
            // A class-wide column has one cell per period whatever the term
            // says; looking it up per subject would find nothing at all.
            if (allSubjectsSource != null && !allSubjectsSource.isSubjectScoped()) {
                return Collections.singletonList(null);
            }
            return ctx.getSubjectIds();
        }
        ComponentDef source = ctx.getGraph().byId(sourceComponentId);
        if (source != null && !source.isSubjectScoped()) {
            return Collections.singletonList(null);
        }
        if (cell.getSubjectId() == null && source != null) {
            // A class-wide column reading a per-subject one: every subject's
            // cell feeds in, collapsed by the term's own reduce.
            return ctx.getSubjectIds();
        }
        return Collections.singletonList(cell.getSubjectId());
    }

    // -------------------------------------------------------------- reduce

    private ReduceOutcome reduceTerm(TermDef term,
                                     List<ResolvedSource> resolved,
                                     NullPolicy nullPolicy,
                                     EvaluationContext ctx,
                                     boolean trace) {
        List<BigDecimal> usable = new ArrayList<>();
        List<SourceTrace> traces = trace ? new ArrayList<>() : Collections.emptyList();

        for (ResolvedSource source : resolved) {
            String code = ctx.getGraph().byId(source.componentId).getCode();
            Optional<CellValue> held = source.value;

            if (!held.isPresent() || held.get().isEmpty()) {
                if (nullPolicy == NullPolicy.BLOCK) {
                    return ReduceOutcome.blocked(addTrace(trace, traces, code, source, null, "BLOCKED", null));
                }
                if (nullPolicy == NullPolicy.AS_ZERO) {
                    usable.add(BigDecimal.ZERO);
                    addTrace(trace, traces, code, source, BigDecimal.ZERO, "EMPTY_AS_ZERO", null);
                } else {
                    addTrace(trace, traces, code, source, null, "EMPTY", null);
                }
                continue;
            }

            CellValue value = held.get();
            if (value.isSpecial()) {
                SpecialValueBehaviour behaviour = ctx.behaviourOf(value.getSpecialValue());
                if (behaviour == SpecialValueBehaviour.BLOCK) {
                    return ReduceOutcome.blocked(addTrace(trace, traces, code, source, null,
                            "BLOCKED", value.getSpecialValue()));
                }
                if (behaviour == SpecialValueBehaviour.AS_ZERO) {
                    usable.add(BigDecimal.ZERO);
                    addTrace(trace, traces, code, source, BigDecimal.ZERO,
                            "SPECIAL_AS_ZERO", value.getSpecialValue());
                } else {
                    addTrace(trace, traces, code, source, null,
                            "SPECIAL_EXCLUDED", value.getSpecialValue());
                }
                continue;
            }

            usable.add(value.getValue());
            addTrace(trace, traces, code, source, value.getValue(), "USED", null);
        }

        return new ReduceOutcome(reduce(term.getReduce(), usable), false, traces);
    }

    private BigDecimal reduce(ReduceType type, List<BigDecimal> values) {
        if (type == ReduceType.COUNT) {
            return BigDecimal.valueOf(values.size());
        }
        if (values.isEmpty()) {
            return null;
        }
        switch (type) {
            case AVERAGE:
                return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(values.size()), MC);
            case SUM:
                return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            case MIN:
                return Collections.min(values);
            case MAX:
                return Collections.max(values);
            case FIRST_NON_NULL:
                return values.get(0);
            case LATEST:
                return values.get(values.size() - 1);
            default:
                throw new TemplateGraphException("unsupported reduce type: " + type);
        }
    }

    private List<SourceTrace> addTrace(boolean trace, List<SourceTrace> traces, String code,
                                       ResolvedSource source, BigDecimal value,
                                       String status, String special) {
        if (trace) {
            traces.add(new SourceTrace(code, source.periodId, source.subjectId, value, status, special));
        }
        return traces;
    }

    // ------------------------------------------------------------ internals

    private static final class ResolvedSource {
        final Long componentId;
        final Long periodId;
        final Long subjectId;
        final Optional<CellValue> value;

        ResolvedSource(Long componentId, Long periodId, Long subjectId, Optional<CellValue> value) {
            this.componentId = componentId;
            this.periodId = periodId;
            this.subjectId = subjectId;
            this.value = value;
        }
    }

    private static final class ReduceOutcome {
        final BigDecimal value;
        final boolean blocked;
        final List<SourceTrace> traces;

        ReduceOutcome(BigDecimal value, boolean blocked, List<SourceTrace> traces) {
            this.value = value;
            this.blocked = blocked;
            this.traces = traces;
        }

        static ReduceOutcome blocked(List<SourceTrace> traces) {
            return new ReduceOutcome(null, true, traces);
        }
    }

    private static final class RuleOutcome {
        final BigDecimal value;
        final List<TermTrace> termTraces;

        RuleOutcome(BigDecimal value, List<TermTrace> termTraces) {
            this.value = value;
            this.termTraces = termTraces;
        }

        static RuleOutcome none(List<TermTrace> termTraces) {
            return new RuleOutcome(null, termTraces);
        }
    }

    /**
     * Kept so callers can round consistently with the engine.
     */
    public static BigDecimal round(BigDecimal value, int decimals, RoundingMode mode) {
        return value == null ? null : value.setScale(decimals, mode);
    }
}
