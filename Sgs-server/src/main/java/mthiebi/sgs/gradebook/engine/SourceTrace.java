package mthiebi.sgs.gradebook.engine;

import lombok.Value;

import java.math.BigDecimal;

/**
 * One input as the evaluator saw it, for the explain popover.
 */
@Value
public class SourceTrace {
    String componentCode;
    Long periodId;
    Long subjectId;
    BigDecimal value;
    /**
     * USED, EMPTY, SPECIAL_EXCLUDED, SPECIAL_AS_ZERO, BLOCKED
     */
    String status;
    String specialValue;
}
