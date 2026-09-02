package mthiebi.sgs.gradebook.engine;

import lombok.Value;
import mthiebi.sgs.gradebook.model.RuleType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Why a derived cell holds the value it does.
 * <p>
 * Produced by the same evaluate() call that computes the value, in trace mode -
 * never by a second implementation, which would drift from the calculation and
 * be believed anyway.
 */
@Value
public class EvaluationTrace {
    String componentCode;
    RuleType ruleType;
    List<TermTrace> terms;
    /**
     * Set when the chain fell through to a fallback rule.
     */
    boolean usedFallback;
    BigDecimal raw;
    BigDecimal value;
    RoundingMode roundingMode;
    int decimals;
}
