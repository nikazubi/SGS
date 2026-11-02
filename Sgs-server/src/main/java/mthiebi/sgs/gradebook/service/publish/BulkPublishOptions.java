package mthiebi.sgs.gradebook.service.publish;

import lombok.AllArgsConstructor;
import lombok.Data;
import mthiebi.sgs.gradebook.service.grid.ClassGroupOption;
import mthiebi.sgs.gradebook.service.grid.PeriodOption;

import java.util.List;

/**
 * What the bulk release dialog offers: the classes this user may publish for,
 * the journals, and the year's periods.
 */
@Data
@AllArgsConstructor
public class BulkPublishOptions {

    private List<ClassGroupOption> classes;
    private List<Journal> journals;
    private List<PeriodOption> periods;

    @Data
    @AllArgsConstructor
    public static class Journal {
        private String uuid;
        private String name;
        /**
         * Grades lock and email on release; the absence register does neither.
         */
        private boolean locksOnPublish;
    }
}
