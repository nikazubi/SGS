package mthiebi.sgs.gradebook.engine;

import mthiebi.sgs.gradebook.model.ComponentKind;
import mthiebi.sgs.gradebook.model.NullPolicy;
import mthiebi.sgs.gradebook.model.PeriodKind;
import mthiebi.sgs.gradebook.model.PeriodRef;
import mthiebi.sgs.gradebook.model.ReduceType;
import mthiebi.sgs.gradebook.model.RuleType;
import mthiebi.sgs.gradebook.model.SourceKind;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A small but complete template, shaped like the real trimester gradebook, so
 * the engine tests read as statements about grading rather than about plumbing.
 * <p>
 * YEAR
 * |- T1 -- SEP_OCT, NOV
 * +- T2 -- DEC, MAR
 * <p>
 * ONGOING_1..3     input,   at T level
 * ONGOING_AVG      derived, average of the three
 * FINAL_TEST       input,   at T level
 * TRIMESTER_GRADE  derived, 0.70 x ONGOING_AVG + 0.30 x FINAL_TEST
 * ANNUAL           derived, at YEAR, average of TRIMESTER_GRADE over children
 * RATING           derived, at YEAR, not subject scoped, average of
 * TRIMESTER_GRADE across every subject
 */
public final class TemplateFixture {

    public static final Long ENROLLMENT = 1L;
    public static final Long MATHS = 10L;
    public static final Long PHYSICS = 11L;

    public static final Long YEAR = 100L;
    public static final Long T1 = 101L;
    public static final Long T2 = 102L;
    public static final Long SEP_OCT = 1011L;
    public static final Long NOV = 1012L;

    public static final Long ONGOING_1 = 1L;
    public static final Long ONGOING_2 = 2L;
    public static final Long ONGOING_3 = 3L;
    public static final Long ONGOING_AVG = 4L;
    public static final Long FINAL_TEST = 5L;
    public static final Long TRIMESTER_GRADE = 6L;
    public static final Long ANNUAL = 7L;
    public static final Long RATING = 8L;

    private TemplateFixture() {
    }

    public static PeriodTree periods() {
        return new PeriodTree(Arrays.asList(
                new PeriodNode(YEAR, null, "YEAR", 0, PeriodKind.YEAR, Arrays.asList(T1, T2)),
                new PeriodNode(T1, YEAR, "T1", 1, PeriodKind.ROLLUP, Arrays.asList(SEP_OCT, NOV)),
                new PeriodNode(T2, YEAR, "T2", 1, PeriodKind.ROLLUP, Collections.emptyList()),
                new PeriodNode(SEP_OCT, T1, "SEP_OCT", 2, PeriodKind.REPORTING, Collections.emptyList()),
                new PeriodNode(NOV, T1, "NOV", 2, PeriodKind.REPORTING, Collections.emptyList())
        ));
    }

    public static TemplateGraph graph() {
        return TemplateGraph.build(components());
    }

    public static List<ComponentDef> components() {
        return Arrays.asList(
                input(ONGOING_1, "ONGOING_1", PeriodKind.ROLLUP, true),
                input(ONGOING_2, "ONGOING_2", PeriodKind.ROLLUP, true),
                input(ONGOING_3, "ONGOING_3", PeriodKind.ROLLUP, true),
                derived(ONGOING_AVG, "ONGOING_AVG", PeriodKind.ROLLUP, true, ongoingAverage()),
                input(FINAL_TEST, "FINAL_TEST", PeriodKind.ROLLUP, true),
                derived(TRIMESTER_GRADE, "TRIMESTER_GRADE", PeriodKind.ROLLUP, true, trimesterGrade()),
                derived(ANNUAL, "ANNUAL", PeriodKind.YEAR, true, annual()),
                derived(RATING, "RATING", PeriodKind.YEAR, false, rating())
        );
    }

    // ------------------------------------------------------------- rules

    public static RuleDef ongoingAverage() {
        return rule(RuleType.AVERAGE, NullPolicy.IGNORE, false, RoundingMode.HALF_UP, 2,
                Collections.singletonList(
                        group(ReduceType.AVERAGE, PeriodRef.SAME,
                                Arrays.asList(ONGOING_1, ONGOING_2, ONGOING_3), "ongoing 1-3")),
                null);
    }

    public static RuleDef trimesterGrade() {
        return rule(RuleType.WEIGHTED_SUM, NullPolicy.IGNORE, true, RoundingMode.HALF_UP, 1,
                Arrays.asList(
                        weighted("0.70", ONGOING_AVG, "ongoing average"),
                        weighted("0.30", FINAL_TEST, "final test")),
                null);
    }

    public static RuleDef annual() {
        return rule(RuleType.AVERAGE, NullPolicy.IGNORE, false, RoundingMode.HALF_UP, 1,
                Collections.singletonList(
                        new TermDef(0, BigDecimal.ONE, SourceKind.COMPONENT, ReduceType.AVERAGE,
                                PeriodRef.CHILDREN, null,
                                Collections.singletonList(TRIMESTER_GRADE), "trimesters")),
                null);
    }

    public static RuleDef rating() {
        return rule(RuleType.AVERAGE, NullPolicy.IGNORE, false, RoundingMode.HALF_UP, 0,
                Collections.singletonList(
                        new TermDef(0, BigDecimal.ONE, SourceKind.ALL_SUBJECTS, ReduceType.AVERAGE,
                                PeriodRef.CHILDREN, null,
                                Collections.singletonList(TRIMESTER_GRADE), "all subjects")),
                null);
    }

    // ------------------------------------------------------------ builders

    public static ComponentDef input(Long id, String code, PeriodKind kind, boolean subjectScoped) {
        return new ComponentDef(id, code, code, id.intValue(), ComponentKind.INPUT,
                kind, subjectScoped, false, 2, null, null, true, null);
    }

    public static ComponentDef derived(Long id, String code, PeriodKind kind,
                                       boolean subjectScoped, RuleDef rule) {
        return new ComponentDef(id, code, code, id.intValue(), ComponentKind.DERIVED,
                kind, subjectScoped, true, rule.getDecimals(), null, null, true, rule);
    }

    public static RuleDef rule(RuleType type, NullPolicy nullPolicy, boolean renormalize,
                               RoundingMode mode, int decimals, List<TermDef> terms, RuleDef fallback) {
        return new RuleDef(type, nullPolicy, renormalize, mode, decimals, terms, fallback);
    }

    public static TermDef weighted(String weight, Long componentId, String label) {
        return new TermDef(0, new BigDecimal(weight), SourceKind.COMPONENT, ReduceType.FIRST_NON_NULL,
                PeriodRef.SAME, null, Collections.singletonList(componentId), label);
    }

    public static TermDef group(ReduceType reduce, PeriodRef ref, List<Long> componentIds, String label) {
        return new TermDef(0, BigDecimal.ONE, SourceKind.GROUP, reduce, ref, null, componentIds, label);
    }

    // ------------------------------------------------------------- context

    public static EvaluationContext context(WorkingSet cells) {
        return context(cells, Collections.singletonList(MATHS), defaultSpecials());
    }

    public static EvaluationContext context(WorkingSet cells, List<Long> subjectIds,
                                            Map<String, SpecialValueBehaviour> specials) {
        return new EvaluationContext(graph(), periods(), cells, subjectIds, specials);
    }

    public static Map<String, SpecialValueBehaviour> defaultSpecials() {
        Map<String, SpecialValueBehaviour> map = new HashMap<>();
        map.put("CHT", SpecialValueBehaviour.EXCLUDE);
        return map;
    }

    public static CellKey cell(Long subjectId, Long periodId, Long componentId) {
        return new CellKey(ENROLLMENT, subjectId, periodId, componentId);
    }

    public static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
