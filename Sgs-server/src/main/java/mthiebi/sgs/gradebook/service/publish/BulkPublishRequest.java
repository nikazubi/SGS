package mthiebi.sgs.gradebook.service.publish;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Several releases at once: one period, and the (class, journal) pairs to
 * release it for.
 * <p>
 * Pairs rather than a list of classes and a list of journals, so a retry can
 * send back only the pairs that failed.
 */
@Data
public class BulkPublishRequest {

    private Long periodId;

    private List<Target> targets = new ArrayList<>();

    @Data
    public static class Target {
        private Long classGroupId;
        private String journalUuid;
    }
}
