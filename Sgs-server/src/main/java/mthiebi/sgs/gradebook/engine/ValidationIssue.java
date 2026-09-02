package mthiebi.sgs.gradebook.engine;

import lombok.Value;

/**
 * One problem with a template version, addressed to whoever is editing it.
 * <p>
 * Errors block activation; warnings do not. The distinction matters because
 * some things that look wrong are legitimate - weights totalling more than
 * 100% is how a bonus scheme is expressed.
 */
@Value
public class ValidationIssue {

    public enum Severity {ERROR, WARNING}

    Severity severity;
    /**
     * Stable key so the UI can localise the message.
     */
    String code;
    String componentCode;
    String message;

    public static ValidationIssue error(String code, String componentCode, String message) {
        return new ValidationIssue(Severity.ERROR, code, componentCode, message);
    }

    public static ValidationIssue warning(String code, String componentCode, String message) {
        return new ValidationIssue(Severity.WARNING, code, componentCode, message);
    }
}
