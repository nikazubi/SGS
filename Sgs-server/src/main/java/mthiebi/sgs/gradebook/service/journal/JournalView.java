package mthiebi.sgs.gradebook.service.journal;

import lombok.AllArgsConstructor;
import lombok.Data;
import mthiebi.sgs.gradebook.model.JournalFrequency;

/**
 * A journal as the menu and the index page see it.
 */
@Data
@AllArgsConstructor
public class JournalView {
    private String uuid;
    private String name;
    private String description;
    private JournalFrequency frequency;
    private boolean subjectScoped;
    private int sortIndex;
    private boolean archived;
    private boolean parentVisible;
    private String chartKey;
    /**
     * Which way round the grid is drawn. The menu reads it to choose between the
     * ordinary gradebook and the transposed absence register.
     */
    private String gridMode;
    /**
     * The version a new period would use.
     */
    private Long currentVersionId;
    private int currentVersionNo;
    private int columnCount;
    /**
     * DRAFT, ACTIVE, LOCKED or ARCHIVED - the status of that version.
     * <p>
     * The menu needs it: a journal whose only version is a draft has no grid to
     * draw, and currentVersionId falls back to the newest version when none is
     * active, so the id alone cannot tell them apart. Null when the journal has
     * no version at all.
     */
    private String currentVersionStatus;
}
