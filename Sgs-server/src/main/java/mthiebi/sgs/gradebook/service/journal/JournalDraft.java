package mthiebi.sgs.gradebook.service.journal;

import lombok.Data;
import mthiebi.sgs.gradebook.model.JournalFrequency;

/**
 * What the wizard collects before any column is defined.
 * <p>
 * Frequency and shape come first deliberately: both change what a column means,
 * and answering them last would let someone build twelve columns before finding
 * out the grid is the wrong shape.
 */
@Data
public class JournalDraft {
    private String name;
    private String description;
    private JournalFrequency frequency = JournalFrequency.ONCE_A_YEAR;
    private boolean subjectScoped = true;
    /**
     * Off by default: a journal is a staff working document until someone
     * decides parents should see it.
     */
    private boolean parentVisible;
    /**
     * Names a chart the parent view draws. Null renders a complete page without one.
     */
    private String chartKey;

}
