package mthiebi.sgs.gradebook.engine;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationResult {

    private final List<ValidationIssue> issues;

    public ValidationResult(List<ValidationIssue> issues) {
        this.issues = issues;
    }

    public List<ValidationIssue> issues() {
        return Collections.unmodifiableList(issues);
    }

    public List<ValidationIssue> errors() {
        return issues.stream()
                .filter(i -> i.getSeverity() == ValidationIssue.Severity.ERROR)
                .collect(Collectors.toList());
    }

    public List<ValidationIssue> warnings() {
        return issues.stream()
                .filter(i -> i.getSeverity() == ValidationIssue.Severity.WARNING)
                .collect(Collectors.toList());
    }

    /**
     * Whether the version may be activated.
     */
    public boolean isActivatable() {
        return errors().isEmpty();
    }
}
