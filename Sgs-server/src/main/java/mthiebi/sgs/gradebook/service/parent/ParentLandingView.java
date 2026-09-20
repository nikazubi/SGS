package mthiebi.sgs.gradebook.service.parent;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * One button on the parent's landing page, and everything the page it opens
 * needs to know before it asks for any marks.
 * <p>
 * Either a configured {@code sgs.parent_view} row or, for a journal that has
 * none, the journal standing in for itself - so the console does not have to
 * know which it is looking at.
 */
@Data
@AllArgsConstructor
public class ParentLandingView {

    /**
     * The words on the button.
     */
    private String label;

    private String journalUuid;

    /**
     * The journal's own name, which heads the page the button opens.
     */
    private String journalName;

    /**
     * Which tier of period to open on - REPORTING, ROLLUP, YEAR - or null to let
     * the page choose, which is what a journal standing in for itself gets.
     */
    private String periodKind;

    /**
     * ONE or ALL. Only meaningful when the journal is subject-scoped.
     */
    private String subjectMode;

    /**
     * Null draws no chart, which is a complete page rather than a broken one.
     */
    private String chartKey;

    /**
     * Which column that chart plots, by code. Null lets the chart choose.
     */
    private String chartColumn;

    /**
     * Whether a subject picker exists at all on the page this opens.
     */
    private boolean subjectScoped;
}
