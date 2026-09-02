package mthiebi.sgs.gradebook.service.parent;

import lombok.AllArgsConstructor;
import lombok.Data;
import mthiebi.sgs.gradebook.model.JournalFrequency;

/**
 * One box on the parent's landing page.
 */
@Data
@AllArgsConstructor
public class ParentJournal {
    private String uuid;
    private String name;
    private String description;
    private JournalFrequency frequency;
    /**
     * Decides whether the view offers a period picker or lists periods as rows.
     */
    private boolean subjectScoped;
    /**
     * Null means the page draws no chart, which is a complete page.
     */
    private String chartKey;
}
