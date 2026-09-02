package mthiebi.sgs.gradebook.model;

/**
 * How the several sources inside one term collapse to a single number.
 */
public enum ReduceType {
    AVERAGE,
    SUM,
    MIN,
    MAX,
    LATEST,
    FIRST_NON_NULL,
    COUNT
}
