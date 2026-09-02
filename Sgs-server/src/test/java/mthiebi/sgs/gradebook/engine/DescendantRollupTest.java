package mthiebi.sgs.gradebook.engine;

import mthiebi.sgs.gradebook.model.ComponentKind;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A total that reaches further than one level.
 * <p>
 * CHILDREN is exactly one level, which is all the trimester journal ever needed:
 * a year's annual mark reads its trimesters. The absence registers are further
 * apart - a yearly total over *days* is three levels, with trimesters and months
 * in between holding nothing.
 * <p>
 * Written as CHILDREN those totals silently never compute. This is the test that
 * would have caught it, and the reason PeriodRef.DESCENDANTS exists.
 */
class DescendantRollupTest {

    private static final Long ENROLLMENT = 1L;
    private static final Long YEAR = 200L;
    private static final Long TRIMESTER = 201L;
    private static final Long MONTH = 202L;
    private static final Long DAY_1 = 2031L;
    private static final Long DAY_2 = 2032L;
    private static final Long DAY_3 = 2033L;

    private static final Long ABSENT = 1L;
    private static final Long DAYS_ABSENT = 2L;

    /**
     * The real absence shape: year, trimester, month, days.
     */
    private PeriodTree periods() {
        return new PeriodTree(Arrays.asList(
                new PeriodNode(YEAR, null, "YEAR", 0, PeriodKind.YEAR,
                        Collections.singletonList(TRIMESTER)),
                new PeriodNode(TRIMESTER, YEAR, "T1", 1, PeriodKind.ROLLUP,
                        Collections.singletonList(MONTH)),
                new PeriodNode(MONTH, TRIMESTER, "SEP", 2, PeriodKind.REPORTING,
                        Arrays.asList(DAY_1, DAY_2, DAY_3)),
                new PeriodNode(DAY_1, MONTH, "2025-09-01", 3, PeriodKind.REPORTING,
                        Collections.emptyList()),
                new PeriodNode(DAY_2, MONTH, "2025-09-02", 3, PeriodKind.REPORTING,
                        Collections.emptyList()),
                new PeriodNode(DAY_3, MONTH, "2025-09-03", 3, PeriodKind.REPORTING,
                        Collections.emptyList())
        ));
    }

    @Test
    @DisplayName("a yearly total sums days three levels below it")
    void yearlyTotalReachesDays() {
        WorkingSet set = twoDaysMarked();
        EvaluationContext ctx = context(set, PeriodRef.DESCENDANTS);

        BigDecimal total = new Evaluator()
                .evaluate(cell(YEAR, DAYS_ABSENT), ctx.getGraph().byId(DAYS_ABSENT), ctx)
                .getValue();

        assertEquals(0, total.compareTo(new BigDecimal("2")),
                "two days marked, so the year says two");
    }

    @Test
    @DisplayName("written as CHILDREN the same total is blank forever")
    void childrenCannotReachDays() {
        // The defect this test documents. The year's children are trimesters,
        // and no day mark lives on a trimester - so the sum sees nothing, on
        // every recompute, silently and permanently.
        WorkingSet set = twoDaysMarked();
        EvaluationContext ctx = context(set, PeriodRef.CHILDREN);

        assertNull(new Evaluator()
                .evaluate(cell(YEAR, DAYS_ABSENT), ctx.getGraph().byId(DAYS_ABSENT), ctx)
                .getValue());
    }

    @Test
    @DisplayName("marking a day recomputes the year, not the month it sits in")
    void recomputeTargetsTheYear() {
        WorkingSet set = twoDaysMarked();
        EvaluationContext ctx = context(set, PeriodRef.DESCENDANTS);

        List<CellKey> affected = new RecomputeEngine()
                .recompute(Collections.singletonList(cell(DAY_1, ABSENT)), ctx);

        // One hop up would put the total on the month - a level the column does
        // not live at, which is invisible data rather than a wrong number.
        assertTrue(affected.stream().anyMatch(c -> DAYS_ABSENT.equals(c.getComponentId())
                        && YEAR.equals(c.getPeriodId())),
                "the year's total is recomputed: " + affected);
        assertTrue(affected.stream().noneMatch(c -> DAYS_ABSENT.equals(c.getComponentId())
                        && MONTH.equals(c.getPeriodId())),
                "and nothing is written at the month: " + affected);
    }

    // ---- fixture -----------------------------------------------------------

    private WorkingSet twoDaysMarked() {
        Map<CellKey, CellValue> map = new HashMap<>();
        map.put(cell(DAY_1, ABSENT), CellValue.manual(BigDecimal.ONE));
        map.put(cell(DAY_3, ABSENT), CellValue.manual(BigDecimal.ONE));
        return new WorkingSet(map);
    }

    private TemplateGraph graph(PeriodRef ref) {
        ComponentDef absent = new ComponentDef(ABSENT, "ABSENT", "გაცდენა", 0,
                ComponentKind.INPUT, PeriodKind.ROLLUP, false, false, 0,
                BigDecimal.ZERO, BigDecimal.ONE, false, null);

        RuleDef rule = new RuleDef(RuleType.SUM, NullPolicy.IGNORE, false,
                RoundingMode.HALF_UP, 0,
                Collections.singletonList(new TermDef(0, BigDecimal.ONE, SourceKind.COMPONENT,
                        ReduceType.SUM, ref, null,
                        Collections.singletonList(ABSENT), "დღეები")),
                null);

        ComponentDef total = new ComponentDef(DAYS_ABSENT, "DAYS_ABSENT", "გაცდენილი დღეები", 1,
                ComponentKind.DERIVED, PeriodKind.YEAR, false, true, 0,
                null, null, false, rule);

        return TemplateGraph.build(Arrays.asList(absent, total));
    }

    private EvaluationContext context(WorkingSet set, PeriodRef ref) {
        EvaluationContext ctx = new EvaluationContext(graph(ref), periods(), set,
                Collections.singletonList(null), new HashMap<>());
        // A three-level reach. No seeded journal needs one now that daily
        // absence has left the period tree, but the engine still supports it
        // and this is what pins that it works.
        ctx.setJournalDepth(3);
        return ctx;
    }

    /**
     * Absence is class-wide, so every cell is subject-null.
     */
    private CellKey cell(Long periodId, Long componentId) {
        return new CellKey(ENROLLMENT, null, periodId, componentId);
    }
}
