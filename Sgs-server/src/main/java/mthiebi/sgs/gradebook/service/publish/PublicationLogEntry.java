package mthiebi.sgs.gradebook.service.publish;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 * One row of the release log.
 */
@Data
@AllArgsConstructor
public class PublicationLogEntry {
    private Long id;
    private String className;
    private String periodLabel;
    /**
     * Null when the whole class was released, which is the usual case.
     */
    private String subjectName;
    private Instant publishedAt;
    private int cellCount;
}
