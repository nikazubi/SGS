package mthiebi.sgs.gradebook.service;

/**
 * Why one cell of a batch was not applied.
 * <p>
 * Reported per cell rather than failing the request: cells are independent, so
 * one refused mark must not discard a teacher's other twenty.
 */
public enum CellRejectionReason {

    /**
     * Someone else changed this cell after the client last read it.
     */
    VERSION_CONFLICT,

    /**
     * The cell has been published to parents. Changing it now goes through a
     * change request and a director's signature, not a direct edit.
     */
    PUBLISHED,

    /**
     * A calculated column configured with {@code allowOverride = false}.
     * <p>
     * Once columns come from live configuration this is no longer necessarily a
     * client bug - a template edit can revoke overriding while a teacher has
     * the grid open - so it is refused per cell rather than thrown.
     */
    NOT_EDITABLE,

    /**
     * Outside the column's own scale - 999 on a grade marked 0 to 10.
     */
    OUT_OF_RANGE,

    /**
     * A special code the template does not declare. Accepting one would make it
     * aggregate by whatever the engine assumes rather than by what was
     * configured - which is how the legacy -50 sentinel came to be averaged as
     * a number on some screens and rendered as text on others.
     */
    INVALID_VALUE
}
