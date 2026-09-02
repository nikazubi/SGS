package mthiebi.sgs.gradebook.service;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CellResult {
    private Long enrollmentId;
    private Long subjectId;
    private Long periodId;
    private String componentCode;
    private BigDecimal value;
    private String specialValue;
    private Integer rowVersion;
}
