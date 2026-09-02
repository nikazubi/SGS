package mthiebi.sgs.gradebook.engine;

import mthiebi.sgs.gradebook.model.PeriodKind;
import mthiebi.sgs.gradebook.model.PeriodRef;
import mthiebi.sgs.gradebook.model.RuleType;
import mthiebi.sgs.gradebook.model.SourceKind;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks a template version before it can be activated.
 * <p>
 * This is the whole reason the evaluator has no error handling for bad
 * configuration: every way a template can be unexecutable is caught here, at
 * the moment someone edits it, rather than months later in front of a teacher
 * entering marks.
 */
public class TemplateValidator {

    private static final BigDecimal ONE = BigDecimal.ONE;
    /**
     * Weights are stored to four places; allow for that when totalling.
     */
    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.0001");

    public ValidationResult validate(List<ComponentDef> components) {
        List<ValidationIssue> issues = new ArrayList<>();

        Set<String> seenCodes = new HashSet<>();
        Set<Long> knownIds = new HashSet<>();
        for (ComponentDef component : components) {
            knownIds.add(component.getId());
            if (!seenCodes.add(component.getCode())) {
                issues.add(ValidationIssue.error("DUPLICATE_CODE", component.getCode(),
                        "More than one column uses the code " + component.getCode()));
            }
        }

        for (ComponentDef component : components) {
            validateComponent(component, knownIds, issues);
        }

        // Structural checks last: they can only run on a graph that builds, and
        // reporting "there is a cycle" on top of ten dangling references is noise.
        if (issues.stream().noneMatch(i -> i.getSeverity() == ValidationIssue.Severity.ERROR)) {
            try {
                TemplateGraph.build(components);
            } catch (TemplateGraphException e) {
                issues.add(ValidationIssue.error("GRAPH", null, e.getMessage()));
            }
        }

        return new ValidationResult(issues);
    }

    private void validateComponent(ComponentDef component, Set<Long> knownIds,
                                   List<ValidationIssue> issues) {
        String code = component.getCode();
        RuleDef rule = component.getRule();

        switch (component.getKind()) {
            case DERIVED:
                if (rule == null) {
                    issues.add(ValidationIssue.error("DERIVED_WITHOUT_RULE", code,
                            "Column " + code + " is calculated but has no formula"));
                    return;
                }
                break;
            case INPUT:
                if (rule != null) {
                    issues.add(ValidationIssue.error("INPUT_WITH_RULE", code,
                            "Column " + code + " is entered by hand but carries a formula"));
                }
                return;
            default:
                return;
        }

        RuleDef current = rule;
        while (current != null) {
            validateRule(component, current, knownIds, issues);
            current = current.getFallback();
        }
    }

    private void validateRule(ComponentDef component, RuleDef rule, Set<Long> knownIds,
                              List<ValidationIssue> issues) {
        String code = component.getCode();

        if (rule.getTerms() == null || rule.getTerms().isEmpty()) {
            issues.add(ValidationIssue.error("RULE_WITHOUT_TERMS", code,
                    "The formula for " + code + " has no inputs"));
            return;
        }

        BigDecimal totalWeight = BigDecimal.ZERO;

        for (TermDef term : rule.getTerms()) {
            if (term.getSourceComponentIds() == null || term.getSourceComponentIds().isEmpty()) {
                issues.add(ValidationIssue.error("TERM_WITHOUT_SOURCES", code,
                        "A part of the formula for " + code + " selects no columns"));
                continue;
            }

            for (Long sourceId : term.getSourceComponentIds()) {
                if (!knownIds.contains(sourceId)) {
                    issues.add(ValidationIssue.error("UNKNOWN_SOURCE", code,
                            "The formula for " + code + " uses a column that no longer exists"));
                }
            }

            if (term.getSourceKind() == SourceKind.ALL_SUBJECTS && component.isSubjectScoped()) {
                issues.add(ValidationIssue.error("ALL_SUBJECTS_ON_SUBJECT_COLUMN", code,
                        "Column " + code + " belongs to a single subject, so it cannot average "
                                + "across every subject"));
            }

            if (term.getSourceKind() == SourceKind.COMPONENT && term.getSourceComponentIds().size() > 1) {
                issues.add(ValidationIssue.error("COMPONENT_TERM_WITH_MANY_SOURCES", code,
                        "A single-column input in " + code + " selects more than one column"));
            }

            if (term.getPeriodRef() == PeriodRef.SPECIFIC && term.getSpecificPeriodId() == null) {
                issues.add(ValidationIssue.error("SPECIFIC_PERIOD_MISSING", code,
                        "The formula for " + code + " refers to a particular period but none is set"));
            }

            // Deliberately not rejected by PeriodKind any more. Months and weeks
            // are both REPORTING once journals can be filled in either way
            // (db/013), so a month column totalling its weeks is a legitimate
            // rollup - and the old check made every such journal impossible to
            // activate. Whether a period actually has children is a property of
            // the scheme, not of the template, so it cannot be decided here.
            if (term.getPeriodRef() == PeriodRef.CHILDREN
                    && component.getPeriodKind() == PeriodKind.YEAR
                    && term.getSourceComponentIds().contains(component.getId())) {
                issues.add(ValidationIssue.error("CHILDREN_SELF_REFERENCE", code,
                        "Column " + code + " totals its own children, which would include itself"));
            }

            totalWeight = totalWeight.add(term.getWeight() == null ? ONE : term.getWeight());
        }

        // A warning, not an error: totals other than 100% are how bonus marks
        // get expressed, and blocking them would be wrong.
        if (rule.getType() == RuleType.WEIGHTED_SUM
                && totalWeight.subtract(ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            issues.add(ValidationIssue.warning("WEIGHTS_NOT_100", code,
                    "The percentages for " + code + " add up to "
                            + totalWeight.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString()
                            + "%, not 100%"));
        }
    }
}
