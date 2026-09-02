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
import java.util.List;
import java.util.stream.Collectors;

import static mthiebi.sgs.gradebook.engine.TemplateFixture.*;
import static org.junit.jupiter.api.Assertions.*;

class TemplateValidatorTest {

    private final TemplateValidator validator = new TemplateValidator();

    private List<String> codesOf(ValidationResult result) {
        return result.issues().stream().map(ValidationIssue::getCode).collect(Collectors.toList());
    }

    @Test
    @DisplayName("the real template passes with no issues at all")
    void acceptsAWellFormedTemplate() {
        ValidationResult result = validator.validate(components());
        assertTrue(result.isActivatable());
        assertTrue(result.issues().isEmpty(), () -> "unexpected issues: " + result.issues());
    }

    @Test
    @DisplayName("weights that do not total 100% warn but do not block")
    void warnsOnWeightsThatDoNotTotal100() {
        ComponentDef odd = derived(TRIMESTER_GRADE, "TRIMESTER_GRADE", PeriodKind.ROLLUP, true,
                rule(RuleType.WEIGHTED_SUM, NullPolicy.IGNORE, true, RoundingMode.HALF_UP, 1,
                        Arrays.asList(weighted("0.70", ONGOING_AVG, "ongoing"),
                                weighted("0.20", FINAL_TEST, "final")),
                        null));

        List<ComponentDef> components = Arrays.asList(
                input(ONGOING_AVG, "ONGOING_AVG", PeriodKind.ROLLUP, true),
                input(FINAL_TEST, "FINAL_TEST", PeriodKind.ROLLUP, true),
                odd);

        ValidationResult result = validator.validate(components);
        assertTrue(result.isActivatable(), "a bonus scheme is legitimate, so this must not block");
        assertEquals(1, result.warnings().size());
        assertEquals("WEIGHTS_NOT_100", result.warnings().get(0).getCode());
        assertTrue(result.warnings().get(0).getMessage().contains("90"),
                result.warnings().get(0).getMessage());
    }

    @Test
    @DisplayName("a calculated column with no formula is rejected")
    void rejectsDerivedWithoutRule() {
        ComponentDef broken = new ComponentDef(1L, "BROKEN", "BROKEN", 1, ComponentKind.DERIVED,
                PeriodKind.ROLLUP, true, true, 2, null, null, true, null);

        ValidationResult result = validator.validate(Collections.singletonList(broken));
        assertFalse(result.isActivatable());
        assertTrue(codesOf(result).contains("DERIVED_WITHOUT_RULE"));
    }

    @Test
    @DisplayName("a hand-entered column carrying a formula is rejected")
    void rejectsInputWithRule() {
        ComponentDef confused = new ComponentDef(1L, "CONFUSED", "CONFUSED", 1, ComponentKind.INPUT,
                PeriodKind.ROLLUP, true, true, 2, null, null, true, ongoingAverage());

        ValidationResult result = validator.validate(Collections.singletonList(confused));
        assertTrue(codesOf(result).contains("INPUT_WITH_RULE"));
    }

    @Test
    @DisplayName("averaging across all subjects is rejected on a per-subject column")
    void rejectsAllSubjectsOnSubjectScopedColumn() {
        ComponentDef wrong = derived(RATING, "RATING", PeriodKind.YEAR, true, rating());

        List<ComponentDef> components = Arrays.asList(
                input(TRIMESTER_GRADE, "TRIMESTER_GRADE", PeriodKind.ROLLUP, true),
                wrong);

        ValidationResult result = validator.validate(components);
        assertFalse(result.isActivatable());
        assertTrue(codesOf(result).contains("ALL_SUBJECTS_ON_SUBJECT_COLUMN"));
    }

    @Test
    @DisplayName("totalling children is rejected on a column at the lowest period")
    void allowsChildrenReferenceAtAReportingPeriod() {
        // Months and weeks are both REPORTING once a journal may be filled in
        // either way, so a month column totalling its weeks is legitimate.
        // Rejecting every REPORTING column made such a journal impossible to
        // activate at all - and whether a period has children is a property of
        // the scheme, which the template cannot see.
        ComponentDef monthly = derived(1L, "MONTH_TOTAL", PeriodKind.REPORTING, true,
                rule(RuleType.AVERAGE, NullPolicy.IGNORE, false, RoundingMode.HALF_UP, 1,
                        Collections.singletonList(
                                new TermDef(0, BigDecimal.ONE, SourceKind.COMPONENT, ReduceType.AVERAGE,
                                        PeriodRef.CHILDREN, null, Collections.singletonList(2L), "weeks")),
                        null));

        List<ComponentDef> components = Arrays.asList(
                monthly, input(2L, "WEEK", PeriodKind.REPORTING, true));

        ValidationResult result = validator.validate(components);
        assertFalse(codesOf(result).contains("CHILDREN_ON_LEAF_PERIOD"));
        assertTrue(result.isActivatable(), codesOf(result).toString());
    }

    @Test
    @DisplayName("a formula pointing at a deleted column is rejected")
    void rejectsUnknownSource() {
        ComponentDef orphan = derived(1L, "ORPHAN", PeriodKind.ROLLUP, true,
                rule(RuleType.AVERAGE, NullPolicy.IGNORE, false, RoundingMode.HALF_UP, 2,
                        Collections.singletonList(group(ReduceType.AVERAGE, PeriodRef.SAME,
                                Collections.singletonList(404L), "gone")),
                        null));

        ValidationResult result = validator.validate(Collections.singletonList(orphan));
        assertFalse(result.isActivatable());
        assertTrue(codesOf(result).contains("UNKNOWN_SOURCE"));
    }

    @Test
    @DisplayName("a cycle is reported once, not on top of every other complaint")
    void reportsCycleWithoutNoise() {
        RuleDef readsB = rule(RuleType.AVERAGE, NullPolicy.IGNORE, false, RoundingMode.HALF_UP, 2,
                Collections.singletonList(group(ReduceType.AVERAGE, PeriodRef.SAME,
                        Collections.singletonList(2L), "b")), null);
        RuleDef readsA = rule(RuleType.AVERAGE, NullPolicy.IGNORE, false, RoundingMode.HALF_UP, 2,
                Collections.singletonList(group(ReduceType.AVERAGE, PeriodRef.SAME,
                        Collections.singletonList(1L), "a")), null);

        ValidationResult result = validator.validate(Arrays.asList(
                derived(1L, "A", PeriodKind.ROLLUP, true, readsB),
                derived(2L, "B", PeriodKind.ROLLUP, true, readsA)));

        assertFalse(result.isActivatable());
        assertEquals(1, result.errors().size());
        assertEquals("GRAPH", result.errors().get(0).getCode());
    }

    @Test
    @DisplayName("a duplicated column code is rejected")
    void rejectsDuplicateCodes() {
        ValidationResult result = validator.validate(Arrays.asList(
                input(1L, "SAME", PeriodKind.ROLLUP, true),
                input(2L, "SAME", PeriodKind.ROLLUP, true)));

        assertFalse(result.isActivatable());
        assertTrue(codesOf(result).contains("DUPLICATE_CODE"));
    }
}
