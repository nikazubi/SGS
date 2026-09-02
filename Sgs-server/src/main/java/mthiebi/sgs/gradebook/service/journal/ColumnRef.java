package mthiebi.sgs.gradebook.service.journal;

import lombok.AllArgsConstructor;
import lombok.Data;
import mthiebi.sgs.gradebook.model.ComponentKind;
import mthiebi.sgs.gradebook.model.JournalFrequency;

/**
 * One entry in the cross-journal column picker.
 */
@Data
@AllArgsConstructor
public class ColumnRef {
    private String journalUuid;
    private String journalName;
    private JournalFrequency journalFrequency;
    private String componentCode;
    private String componentLabel;
    private ComponentKind kind;
    /**
     * True when the two journals are filled in equally often, so a reference
     * needs no period question. False means the picker must ask which
     * occurrence is meant.
     */
    private boolean sameFrequencyAsCaller;
}
