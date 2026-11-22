package mthiebi.sgs.gradebook.service.journal;

import lombok.Data;
import mthiebi.sgs.gradebook.model.ComponentKind;
import mthiebi.sgs.gradebook.model.PeriodKind;

import java.math.BigDecimal;

/**
 * One column as the journal editor sends it.
 * <p>
 * Codes rather than ids throughout: a draft is round-tripped through the browser
 * as JSON and has to survive a column being added, removed and re-added before
 * anything is saved.
 */
@Data
public class ComponentDraft {

    private String code;
    private String label;
    private int ordinal;
    private String groupLabel;

    private ComponentKind kind = ComponentKind.INPUT;
    private PeriodKind periodKind = PeriodKind.ROLLUP;
    private boolean subjectScoped = true;

    private BigDecimal scaleMin;
    private BigDecimal scaleMax;
    private int decimals;

    private boolean allowSpecialValues;
    private boolean allowOverride = true;
    private boolean parentVisible = true;

    /**
     * Shown on the year screen beside the year's columns and in the parents'
     * summary (component.summary_column). A Boolean, not a boolean: null means
     * "not sent, leave it as it is", so a console that predates the field can
     * never switch it off by saving.
     */
    private Boolean summaryColumn;

    /**
     * Null for a column that is typed in.
     */
    private RuleDraft rule;
}
