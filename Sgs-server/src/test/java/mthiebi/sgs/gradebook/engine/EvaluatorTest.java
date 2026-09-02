package mthiebi.sgs.gradebook.engine;

import mthiebi.sgs.gradebook.model.NullPolicy;
import mthiebi.sgs.gradebook.model.PeriodKind;
import mthiebi.sgs.gradebook.model.PeriodRef;
import mthiebi.sgs.gradebook.model.ReduceType;
import mthiebi.sgs.gradebook.model.RuleType;
import mthiebi.sgs.gradebook.model.SourceKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static mthiebi.sgs.gradebook.engine.TemplateFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluatorTest {

    private final Evaluator evaluator = new Evaluator();

    private WorkingSet cells(Object... keyValuePairs) {
        Map<CellKey, CellValue> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put((CellKey) keyValuePairs[i], (CellValue) keyValuePairs[i + 1]);
        }
        return new WorkingSet(map);
    }

    private BigDecimal evaluate(WorkingSet set, Long componentId, Long periodId, Long subjectId) {
        EvaluationContext ctx = context(set);
        ComponentDef component = ctx.getGraph().byId(componentId);
        return evaluator.evaluate(cell(subjectId, periodId, componentId), component, ctx).getValue();
    }

    // ------------------------------------------------------- weighted sums

    @Test
    @DisplayName("weighted sum with every input present")
    void weightedSumAllPresent() {
        WorkingSet set = cells(
                cell(MATHS, T1, ONGOING_AVG), CellValue.derived(bd("8")),
                cell(MATHS, T1, FINAL_TEST), CellValue.manual(bd("6")));

        // 0.70*8 + 0.30*6 = 5.6 + 1.8 = 7.4
        assertEquals(bd("7.4"), evaluate(set, TRIMESTER_GRADE, T1, MATHS));
    }

    @Test
    @DisplayName("a missing term rescales the remaining weights back to 100%")
    void weightedSumRenormalises() {
        WorkingSet set = cells(cell(MATHS, T1, ONGOING_AVG), CellValue.derived(bd("8")));

        // Only the 0.70 term survives. Without renormalisation the student would
        // score 5.6 - capped at 70% of scale for work they were never set.
        assertEquals(bd("8.0"), evaluate(set, TRIMESTER_GRADE, T1, MATHS));
    }

    @Test
    @DisplayName("without renormalisation a missing term silently caps the result")
    void weightedSumWithoutRenormalisation() {
        ComponentDef capped = derived(TRIMESTER_GRADE, "TRIMESTER_GRADE", PeriodKind.ROLLUP, true,
                rule(RuleType.WEIGHTED_SUM, NullPolicy.IGNORE, false, RoundingMode.HALF_UP, 1,
                        Arrays.asList(weighted("0.70", ONGOING_AVG, "ongoing"),
                                weighted("0.30", FINAL_TEST, "final")),
                        null));

        WorkingSet set = cells(cell(MATHS, T1, ONGOING_AVG), CellValue.derived(bd("8")));
        EvaluationContext ctx = context(set);

        assertEquals(bd("5.6"),
                evaluator.evaluate(cell(MATHS, T1, TRIMESTER_GRADE), capped, ctx).getValue());
    }

    // -------------------------------------------------------------- blanks

    @Test
    @DisplayName("IGNORE averages over the marks that exist")
    void averageIgnoresBlanks() {
        WorkingSet set = cells(
                cell(MATHS, T1, ONGOING_1), CellValue.manual(bd("6")),
                cell(MATHS, T1, ONGOING_2), CellValue.manual(bd("8")));

        // (6+8)/2, not (6+8+0)/3
        assertEquals(bd("7.00"), evaluate(set, ONGOING_AVG, T1, MATHS));
    }

    @Test
    @DisplayName("AS_ZERO counts blanks against the student")
    void averageCountsBlanksAsZero() {
        ComponentDef strict = derived(ONGOING_AVG, "ONGOING_AVG", PeriodKind.ROLLUP, true,
                rule(RuleType.AVERAGE, NullPolicy.AS_ZERO, false, RoundingMode.HALF_UP, 2,
                        Collections.singletonList(group(ReduceType.AVERAGE, PeriodRef.SAME,
                                Arrays.asList(ONGOING_1, ONGOING_2, ONGOING_3), "ongoing")),
                        null));

        WorkingSet set = cells(
                cell(MATHS, T1, ONGOING_1), CellValue.manual(bd("6")),
                cell(MATHS, T1, ONGOING_2), CellValue.manual(bd("8")));
        EvaluationContext ctx = context(set);

        // (6+8+0)/3
        assertEquals(bd("4.67"),
                evaluator.evaluate(cell(MATHS, T1, ONGOING_AVG), strict, ctx).getValue());
    }

    @Test
    @DisplayName("no marks at all yields null, never zero")
    void noInputYieldsNull() {
        assertNull(evaluate(cells(), ONGOING_AVG, T1, MATHS));
    }

    // ------------------------------------------------------ special values

    @Test
    @DisplayName("a special value is excluded, counted as zero, or blocks the result")
    void specialValueBehaviours() {
        WorkingSet set = cells(
                cell(MATHS, T1, ONGOING_1), CellValue.manual(bd("6")),
                cell(MATHS, T1, ONGOING_2), CellValue.special("CHT"));

        Map<String, SpecialValueBehaviour> exclude = new HashMap<>();
        exclude.put("CHT", SpecialValueBehaviour.EXCLUDE);
        assertEquals(bd("6.00"), evaluateWithSpecials(set, exclude));

        Map<String, SpecialValueBehaviour> asZero = new HashMap<>();
        asZero.put("CHT", SpecialValueBehaviour.AS_ZERO);
        assertEquals(bd("3.00"), evaluateWithSpecials(set, asZero));

        Map<String, SpecialValueBehaviour> block = new HashMap<>();
        block.put("CHT", SpecialValueBehaviour.BLOCK);
        assertNull(evaluateWithSpecials(set, block));
    }

    private BigDecimal evaluateWithSpecials(WorkingSet set, Map<String, SpecialValueBehaviour> specials) {
        EvaluationContext ctx = context(set, Collections.singletonList(MATHS), specials);
        return evaluator.evaluate(cell(MATHS, T1, ONGOING_AVG),
                ctx.getGraph().byId(ONGOING_AVG), ctx).getValue();
    }

    // ------------------------------------------------------- cross-period

    @Test
    @DisplayName("annual averages the trimesters, ignoring one that is missing")
    void annualOverChildren() {
        WorkingSet set = cells(
                cell(MATHS, T1, TRIMESTER_GRADE), CellValue.derived(bd("7")),
                cell(MATHS, T2, TRIMESTER_GRADE), CellValue.derived(bd("8")));
        assertEquals(bd("7.5"), evaluate(set, ANNUAL, YEAR, MATHS));

        WorkingSet partial = cells(cell(MATHS, T1, TRIMESTER_GRADE), CellValue.derived(bd("7")));
        assertEquals(bd("7.0"), evaluate(partial, ANNUAL, YEAR, MATHS));
    }

    // ------------------------------------------------------ cross-subject

    @Test
    @DisplayName("rating averages across every subject, skipping ones with no marks")
    void ratingAcrossSubjects() {
        WorkingSet set = cells(
                cell(MATHS, T1, TRIMESTER_GRADE), CellValue.derived(bd("7")),
                cell(MATHS, T2, TRIMESTER_GRADE), CellValue.derived(bd("8")),
                cell(PHYSICS, T1, TRIMESTER_GRADE), CellValue.derived(bd("6")));

        EvaluationContext ctx = context(set, Arrays.asList(MATHS, PHYSICS), defaultSpecials());
        // Physics has no T2 mark; the average is over the three that exist.
        BigDecimal value = evaluator.evaluate(cell(null, YEAR, RATING),
                ctx.getGraph().byId(RATING), ctx).getValue();
        assertEquals(bd("7"), value);
    }

    // ---------------------------------------------------------- fallbacks

    @Test
    @DisplayName("the legacy summary chain: avg(S1,S2), else avg(one, resit), else the resit alone")
    void fallbackChain() {
        Long s1 = 20L, s2 = 21L, resit = 22L, summary = 23L;

        RuleDef resitOnly = rule(RuleType.AVERAGE, NullPolicy.IGNORE, false, RoundingMode.HALF_UP, 1,
                Collections.singletonList(group(ReduceType.AVERAGE, PeriodRef.SAME,
                        Collections.singletonList(resit), "resit")), null);

        RuleDef onePlusResit = rule(RuleType.AVERAGE, NullPolicy.IGNORE, false, RoundingMode.HALF_UP, 1,
                Collections.singletonList(group(ReduceType.AVERAGE, PeriodRef.SAME,
                        Arrays.asList(s1, s2, resit), "one plus resit")), resitOnly);

        RuleDef bothPresent = rule(RuleType.AVERAGE, NullPolicy.BLOCK, false, RoundingMode.HALF_UP, 1,
                Collections.singletonList(group(ReduceType.AVERAGE, PeriodRef.SAME,
                        Arrays.asList(s1, s2), "both")), onePlusResit);

        ComponentDef summaryComponent = derived(summary, "SUMMARY", PeriodKind.ROLLUP, true, bothPresent);
        TemplateGraph graph = TemplateGraph.build(Arrays.asList(
                input(s1, "S1", PeriodKind.ROLLUP, true),
                input(s2, "S2", PeriodKind.ROLLUP, true),
                input(resit, "RESIT", PeriodKind.ROLLUP, true),
                summaryComponent));

        assertEquals(bd("7.0"), summaryOf(graph, summaryComponent, cells(
                cell(MATHS, T1, s1), CellValue.manual(bd("6")),
                cell(MATHS, T1, s2), CellValue.manual(bd("8")))));

        // S2 missing, so the first rule blocks and the chain averages S1 with the resit.
        assertEquals(bd("5.0"), summaryOf(graph, summaryComponent, cells(
                cell(MATHS, T1, s1), CellValue.manual(bd("6")),
                cell(MATHS, T1, resit), CellValue.manual(bd("4")))));

        // Only the resit exists.
        assertEquals(bd("4.0"), summaryOf(graph, summaryComponent, cells(
                cell(MATHS, T1, resit), CellValue.manual(bd("4")))));

        assertNull(summaryOf(graph, summaryComponent, cells()));
    }

    private BigDecimal summaryOf(TemplateGraph graph, ComponentDef component, WorkingSet set) {
        EvaluationContext ctx = new EvaluationContext(graph, periods(), set,
                Collections.singletonList(MATHS), defaultSpecials());
        return evaluator.evaluate(cell(MATHS, T1, component.getId()), component, ctx).getValue();
    }

    // ----------------------------------------------------------- rounding

    @Test
    @DisplayName("rounding happens once at the end, not at each step")
    void roundsOnlyAtTheEnd() {
        // ongoing average of 7, 8, 8 is 7.666...; the trimester rule then takes
        // 0.70 of it. Rounding the average to 7.67 first, or to 8, would move
        // the final mark - which is what the old code did inconsistently.
        WorkingSet set = cells(
                cell(MATHS, T1, ONGOING_1), CellValue.manual(bd("7")),
                cell(MATHS, T1, ONGOING_2), CellValue.manual(bd("8")),
                cell(MATHS, T1, ONGOING_3), CellValue.manual(bd("8")));

        assertEquals(bd("7.67"), evaluate(set, ONGOING_AVG, T1, MATHS));
    }

    // -------------------------------------------------------------- trace

    @Test
    @DisplayName("trace mode reports which inputs were used and why others were not")
    void traceExplainsTheResult() {
        WorkingSet set = cells(
                cell(MATHS, T1, ONGOING_1), CellValue.manual(bd("6")),
                cell(MATHS, T1, ONGOING_2), CellValue.special("CHT"));

        EvaluationContext ctx = context(set);
        EvalOutcome outcome = evaluator.evaluate(cell(MATHS, T1, ONGOING_AVG),
                ctx.getGraph().byId(ONGOING_AVG), ctx, true);

        EvaluationTrace trace = outcome.getTrace();
        assertEquals("ONGOING_AVG", trace.getComponentCode());
        assertEquals(1, trace.getTerms().size());

        java.util.List<SourceTrace> sources = trace.getTerms().get(0).getSources();
        assertEquals(3, sources.size());
        assertEquals("USED", sources.get(0).getStatus());
        assertEquals("SPECIAL_EXCLUDED", sources.get(1).getStatus());
        assertEquals("EMPTY", sources.get(2).getStatus());
        assertTrue(trace.getValue().compareTo(bd("6.00")) == 0);
    }

    @Test
    @DisplayName("an input component evaluates to nothing rather than erroring")
    void inputComponentsAreNotEvaluated() {
        EvaluationContext ctx = context(cells());
        EvalOutcome outcome = evaluator.evaluate(cell(MATHS, T1, ONGOING_1),
                ctx.getGraph().byId(ONGOING_1), ctx);
        assertNull(outcome.getValue());
    }
}
