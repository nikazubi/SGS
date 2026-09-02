package mthiebi.sgs.gradebook.service.journal;

import lombok.Data;
import mthiebi.sgs.gradebook.model.NullPolicy;
import mthiebi.sgs.gradebook.model.RuleType;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * How a calculated column is worked out.
 */
@Data
public class RuleDraft {
    private RuleType type = RuleType.WEIGHTED_SUM;
    private NullPolicy nullPolicy = NullPolicy.IGNORE;
    /**
     * On by default: a student missing one input is scored on what they did
     * sit, rather than quietly capped at a fraction of the scale.
     */
    private boolean renormalizeWeights = true;
    private RoundingMode roundingMode = RoundingMode.HALF_UP;
    private int decimals;
    private List<TermDraft> terms = new ArrayList<>();
}
