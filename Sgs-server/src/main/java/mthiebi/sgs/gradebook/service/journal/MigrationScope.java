package mthiebi.sgs.gradebook.service.journal;

import lombok.Data;

/**
 * One (class, period) a migration would touch.
 */
@Data
public class MigrationScope {
    private Long classGroupId;
    private Long periodId;
    private String className;
    private String periodLabel;
    private int subjectCount;
    private int cellCount;
}
