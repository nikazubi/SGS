package mthiebi.sgs.gradebook.model;

public enum PeriodKind {
    /**
     * A period marks are entered against, e.g. SEP_OCT.
     */
    REPORTING,
    /**
     * Aggregates its children, e.g. a trimester.
     */
    ROLLUP,
    /**
     * The root of a scheme.
     */
    YEAR
}
