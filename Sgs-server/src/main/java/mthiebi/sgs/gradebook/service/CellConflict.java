package mthiebi.sgs.gradebook.service;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * One cell of a batch that was not applied, and why - a competing edit, a
 * publication lock, or a column that forbids overriding.
 * <p>
 * Reported per cell rather than failing the batch: cells are independent, so
 * one refused mark should not discard a teacher's other twenty.
 */
@Data
@AllArgsConstructor
public class CellConflict {
    private Long enrollmentId;
    private String componentCode;
    private BigDecimal attempted;
    private BigDecimal current;
    private Integer currentVersion;
    /**
     * What the client should say about it.
     */
    private CellRejectionReason reason;
}
