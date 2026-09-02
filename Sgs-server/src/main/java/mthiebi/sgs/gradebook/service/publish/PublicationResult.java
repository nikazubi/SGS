package mthiebi.sgs.gradebook.service.publish;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class PublicationResult {
    private Long publicationId;
    /**
     * Cells whose released value actually changed.
     */
    private int released;
    /**
     * Cells in scope that held a value at all.
     */
    private int inScope;
    private Instant publishedAt;
}
