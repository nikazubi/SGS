package mthiebi.sgs.gradebook.engine;

import lombok.Value;

import java.math.BigDecimal;

/**
 * The result of evaluating one derived cell.
 * <p>
 * A null value is normal - it means the rule had nothing to work with, which is
 * what a student with no marks yet should produce. Configuration problems are
 * not represented here; those are rejected when the template version is saved.
 */
@Value
public class EvalOutcome {

    BigDecimal value;
    /**
     * Populated only when evaluation ran in trace mode.
     */
    EvaluationTrace trace;

    public static EvalOutcome empty() {
        return new EvalOutcome(null, null);
    }

    public boolean hasValue() {
        return value != null;
    }
}
