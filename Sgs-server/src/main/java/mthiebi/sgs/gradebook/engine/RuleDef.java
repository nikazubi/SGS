package mthiebi.sgs.gradebook.engine;

import lombok.Value;
import mthiebi.sgs.gradebook.model.NullPolicy;
import mthiebi.sgs.gradebook.model.RuleType;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Value
public class RuleDef {
    RuleType type;
    NullPolicy nullPolicy;
    boolean renormalizeWeights;
    RoundingMode roundingMode;
    int decimals;
    List<TermDef> terms;
    /**
     * Tried when this rule yields nothing; null ends the chain.
     */
    RuleDef fallback;

    /**
     * Every component this rule reads, following the fallback chain.
     */
    public List<Long> allSourceComponentIds() {
        List<Long> ids = new ArrayList<>();
        RuleDef current = this;
        while (current != null) {
            for (TermDef term : current.getTerms()) {
                ids.addAll(term.getSourceComponentIds());
            }
            current = current.getFallback();
        }
        return ids;
    }
}
