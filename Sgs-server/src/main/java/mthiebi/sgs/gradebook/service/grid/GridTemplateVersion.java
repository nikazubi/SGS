package mthiebi.sgs.gradebook.service.grid;

import lombok.AllArgsConstructor;
import lombok.Data;
import mthiebi.sgs.gradebook.model.TemplateVersionStatus;

/**
 * Which configuration this grid is being rendered under.
 * <p>
 * {@code pinned} means the period already holds marks and is therefore locked
 * to this version: the template editor needs it before offering to migrate,
 * because an unpinned period can simply be reassigned while a pinned one has
 * marks that would have to be recalculated.
 */
@Data
@AllArgsConstructor
public class GridTemplateVersion {
    private Long id;
    private String templateName;
    private int versionNo;
    private TemplateVersionStatus status;
    private boolean pinned;
}
