package mthiebi.sgs.gradebook.service.publish;

import lombok.Data;

import java.math.BigDecimal;

/**
 * A teacher asking to change a grade parents have already seen.
 */
@Data
public class RaiseChangeRequest {
    private Long gradeEntryId;
    private BigDecimal requestedValue;
    private String requestedSpecialValue;
    /**
     * Required: an unexplained request cannot be judged.
     */
    private String reason;
}
