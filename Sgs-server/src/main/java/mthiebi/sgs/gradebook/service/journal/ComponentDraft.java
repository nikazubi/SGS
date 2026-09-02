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
     * Null for a column that is typed in.
     */
    private RuleDraft rule;
}
