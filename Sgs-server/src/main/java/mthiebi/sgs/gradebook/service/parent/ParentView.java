package mthiebi.sgs.gradebook.service.parent;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * A journal as a parent sees it.
 * <p>
 * Every value here is the published one. A parent never sees work in progress:
 * that is the whole point of publication, and reading the working value would
 * quietly undo it.
 */
@Data
public class ParentView {
    private String journalName;
    private String chartKey;
    private boolean subjectScoped;
    /**
     * Offered only when rows are subjects; a class-wide journal lists periods as rows.
     */
    private final List<ParentPeriod> periods = new ArrayList<>();
    private Long selectedPeriodId;
    private final List<ParentColumn> columns = new ArrayList<>();
    private final List<ParentRow> rows = new ArrayList<>();
}
