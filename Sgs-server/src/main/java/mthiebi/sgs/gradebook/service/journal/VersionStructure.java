package mthiebi.sgs.gradebook.service.journal;

import lombok.Data;
import mthiebi.sgs.gradebook.model.TemplateVersionStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * A whole version, as the editor loads and saves it.
 * <p>
 * Posted entire rather than as per-column CRUD: it is what a wizard and a
 * spreadsheet-shaped editor both produce, and it lets validation see the
 * complete structure instead of judging one column at a time.
 */
@Data
public class VersionStructure {

    private Long versionId;
    private int versionNo;
    private TemplateVersionStatus status;

    /**
     * No grade_entry references this version, so it can be edited directly.
     * Otherwise saving forks a draft, because editing in place would silently
     * re-render marks already entered.
     */
    private boolean editableInPlace;

    private List<ComponentDraft> components = new ArrayList<>();
}
