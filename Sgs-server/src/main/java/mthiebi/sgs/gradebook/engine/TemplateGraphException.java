package mthiebi.sgs.gradebook.engine;

/**
 * A template version whose shape cannot be executed. Never expected at runtime:
 * these conditions are rejected when a version is saved, so a teacher entering
 * marks can never be shown a configuration error.
 */
public class TemplateGraphException extends RuntimeException {

    public TemplateGraphException(String message) {
        super(message);
    }
}
