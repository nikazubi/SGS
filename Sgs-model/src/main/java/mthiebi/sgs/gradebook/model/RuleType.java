package mthiebi.sgs.gradebook.model;

/**
 * How a rule's terms combine into one value.
 */
public enum RuleType {
    /**
     * sum(value * weight); the "col3 = col1 x% + col2 y%" shape.
     */
    WEIGHTED_SUM,
    AVERAGE,
    SUM,
    MIN,
    MAX,
    FIRST_NON_NULL
}
