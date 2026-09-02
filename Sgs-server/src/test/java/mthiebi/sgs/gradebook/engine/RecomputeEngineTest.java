package mthiebi.sgs.gradebook.engine;

import mthiebi.sgs.gradebook.model.GradeSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mthiebi.sgs.gradebook.engine.TemplateFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecomputeEngineTest {

    private final RecomputeEngine engine = new RecomputeEngine();

    private WorkingSet marks() {
        Map<CellKey, CellValue> map = new HashMap<>();
        map.put(cell(MATHS, T1, ONGOING_1), CellValue.manual(bd("6")));
        map.put(cell(MATHS, T1, ONGOING_2), CellValue.manual(bd("8")));
        map.put(cell(MATHS, T1, FINAL_TEST), CellValue.manual(bd("6")));
        return new WorkingSet(map);
    }

    private BigDecimal valueAt(WorkingSet set, Long subject, Long period, Long component) {
        return set.get(cell(subject, period, component)).map(CellValue::getValue).orElse(null);
    }

    @Test
    @DisplayName("one edit cascades along component, period and subject axes")
    void cascadesAcrossAllThreeAxes() {
        WorkingSet set = marks();
        EvaluationContext ctx = context(set);

        List<CellKey> written = engine.recompute(
                Collections.singletonList(cell(MATHS, T1, ONGOING_1)), ctx);

        // component axis: ONGOING_AVG then TRIMESTER_GRADE, both at T1
        assertEquals(bd("7.00"), valueAt(set, MATHS, T1, ONGOING_AVG));
        assertEquals(bd("6.7"), valueAt(set, MATHS, T1, TRIMESTER_GRADE));
        // period axis: the trimester rolls up into the year
        assertEquals(bd("6.7"), valueAt(set, MATHS, YEAR, ANNUAL));
        // subject axis: rating is student-wide, so it has no subject
        assertEquals(bd("7"), valueAt(set, null, YEAR, RATING));

        assertEquals(4, written.size());
    }

    @Test
    @DisplayName("leaf periods are evaluated before the rollups that consume them")
    void ordersDeeperPeriodsFirst() {
        WorkingSet set = marks();
        List<CellKey> written = engine.recompute(
                Collections.singletonList(cell(MATHS, T1, ONGOING_1)), context(set));

        List<Long> order = new java.util.ArrayList<>();
        for (CellKey key : written) {
            order.add(key.getComponentId());
        }
        assertEquals(Arrays.asList(ONGOING_AVG, TRIMESTER_GRADE, ANNUAL, RATING), order);
    }

    @Test
    @DisplayName("an overridden cell keeps its value but still feeds everything downstream")
    void overrideIsPreservedAndPropagates() {
        WorkingSet set = marks();
        set.put(cell(MATHS, T1, TRIMESTER_GRADE),
                new CellValue(bd("9.0"), null, GradeSource.DERIVED, true, 0));

        engine.recompute(Collections.singletonList(cell(MATHS, T1, ONGOING_1)), context(set));

        // Not recomputed back to 6.7 ...
        assertEquals(bd("9.0"), valueAt(set, MATHS, T1, TRIMESTER_GRADE));
        // ... but the year average is built from the value the person typed.
        assertEquals(bd("9.0"), valueAt(set, MATHS, YEAR, ANNUAL));
    }

    @Test
    @DisplayName("clearing an override re-derives the cell")
    void clearingAnOverrideRederives() {
        WorkingSet set = marks();
        EvaluationContext ctx = context(set);

        // Derived values are materialised, so bring the working set to the state
        // it would really be in before anyone overrides anything.
        engine.recompute(Collections.singletonList(cell(MATHS, T1, ONGOING_1)), ctx);
        assertEquals(bd("6.7"), valueAt(set, MATHS, T1, TRIMESTER_GRADE));

        set.put(cell(MATHS, T1, TRIMESTER_GRADE),
                new CellValue(bd("9.0"), null, GradeSource.DERIVED, true, 0));
        engine.recompute(Collections.singletonList(cell(MATHS, T1, ONGOING_1)), ctx);
        assertEquals(bd("9.0"), valueAt(set, MATHS, T1, TRIMESTER_GRADE));

        // Releasing the override hands the cell back to the engine.
        set.put(cell(MATHS, T1, TRIMESTER_GRADE),
                new CellValue(bd("9.0"), null, GradeSource.DERIVED, false, 0));
        engine.recompute(Collections.emptyList(),
                Collections.singletonList(cell(MATHS, T1, TRIMESTER_GRADE)),
                ctx);

        assertEquals(bd("6.7"), valueAt(set, MATHS, T1, TRIMESTER_GRADE));
    }

    @Test
    @DisplayName("recomputing twice writes nothing the second time")
    void isIdempotent() {
        WorkingSet set = marks();
        EvaluationContext ctx = context(set);

        List<CellKey> first = engine.recompute(
                Collections.singletonList(cell(MATHS, T1, ONGOING_1)), ctx);
        assertTrue(first.size() > 0);

        List<CellKey> second = engine.recompute(
                Collections.singletonList(cell(MATHS, T1, ONGOING_1)), ctx);
        assertTrue(second.isEmpty(), "second pass should write nothing, wrote " + second);
    }

    @Test
    @DisplayName("a second subject changes the student-wide rating but not the first subject")
    void secondSubjectOnlyMovesTheSharedAggregate() {
        Map<CellKey, CellValue> map = new HashMap<>();
        map.put(cell(MATHS, T1, TRIMESTER_GRADE), CellValue.derived(bd("8")));
        map.put(cell(PHYSICS, T1, ONGOING_1), CellValue.manual(bd("4")));
        WorkingSet set = new WorkingSet(map);

        EvaluationContext ctx = context(set, Arrays.asList(MATHS, PHYSICS), defaultSpecials());
        engine.recompute(Collections.singletonList(cell(PHYSICS, T1, ONGOING_1)), ctx);

        assertEquals(bd("4.00"), valueAt(set, PHYSICS, T1, ONGOING_AVG));
        // Maths is untouched ...
        assertEquals(bd("8"), valueAt(set, MATHS, T1, TRIMESTER_GRADE));
        // ... while the shared rating sees both subjects: (8 + 4.0) / 2 = 6
        assertEquals(bd("6"), valueAt(set, null, YEAR, RATING));
    }

    @Test
    @DisplayName("removing the last mark leaves the derived cell empty rather than zero")
    void emptyInputsProduceNoValue() {
        WorkingSet set = WorkingSet.empty();
        EvaluationContext ctx = context(set);

        engine.recompute(Collections.singletonList(cell(MATHS, T1, ONGOING_1)), ctx);

        assertNull(valueAt(set, MATHS, T1, ONGOING_AVG));
        assertNull(valueAt(set, MATHS, T1, TRIMESTER_GRADE));
    }
}
