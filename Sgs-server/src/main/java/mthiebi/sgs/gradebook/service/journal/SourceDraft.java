package mthiebi.sgs.gradebook.service.journal;

import lombok.Data;

/**
 * One column feeding a term.
 */
@Data
public class SourceDraft {
    /**
     * Null means this journal. Set, it names another - the picker offers every
     * journal and every column, and a plain mirror is the trivial case of the
     * same mechanism: one term, weight 1.
     */
    private String journalUuid;
    private String componentCode;
}
