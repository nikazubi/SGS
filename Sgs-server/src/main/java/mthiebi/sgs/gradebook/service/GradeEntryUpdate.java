package mthiebi.sgs.gradebook.service;

import lombok.Data;

import java.math.BigDecimal;

/**
 * One cell a person is trying to change.
 */
@Data
public class GradeEntryUpdate {

    private Long enrollmentId;
    private String componentCode;
    private BigDecimal value;
    private String specialValue;

    /**
     * The row version the client last saw. Null skips the check, which is what
     * a first write to an empty cell does.
     */
    private Integer expectedVersion;

    /**
     * Set when typing over a calculated column. Ignored unless the column
     * allows it; clearing it hands the cell back to the engine.
     */
    private Boolean override;
}
