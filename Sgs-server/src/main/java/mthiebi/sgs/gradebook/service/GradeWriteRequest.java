package mthiebi.sgs.gradebook.service;

import lombok.Data;

import java.util.List;

/**
 * One flush of a grid. The client batches dirty cells and sends them on row
 * blur or after a short pause, rather than a request per keystroke - which is
 * what the old console did, and then reloaded the whole grid afterwards.
 */
@Data
public class GradeWriteRequest {

    /**
     * Which journal. Null falls back to the first one in the menu.
     */
    private String journalUuid;
    private Long classGroupId;
    private Long subjectId;
    private Long periodId;
    private List<GradeEntryUpdate> entries;
}
