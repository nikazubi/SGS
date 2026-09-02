package mthiebi.sgs.gradebook.service.journal;

import lombok.AllArgsConstructor;
import lombok.Data;
import mthiebi.sgs.gradebook.engine.ValidationIssue;

import java.util.List;

/**
 * What a save did, and what is wrong with it.
 * <p>
 * Errors block activation but not saving - a half-built formula has to be
 * storable or the editor cannot be left open overnight. Warnings never block:
 * weights totalling more than 100% is how a bonus scheme is expressed.
 */
@Data
@AllArgsConstructor
public class SaveResult {
    private VersionStructure version;
    /**
     * Set when the save forked a draft rather than editing in place.
     */
    private boolean forked;
    private List<ValidationIssue> issues;
    private boolean activatable;
}
