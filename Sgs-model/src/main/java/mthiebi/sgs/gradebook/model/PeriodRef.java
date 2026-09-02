package mthiebi.sgs.gradebook.model;

public enum PeriodRef {
    /**
     * The period being computed.
     */
    SAME,
    /**
     * Every child period, e.g. YEAR.ANNUAL drawing on T1/T2/T3.
     */
    CHILDREN,
    /**
     * Every descendant at the level the source column lives at, however far
     * down that is.
     * <p>
     * CHILDREN is exactly one level, which is all the trimester journal ever
     * needed - a year's annual mark reads its trimesters. The absence registers
     * are further apart: a yearly total over *days* is three levels, and over
     * months is two, with trimesters in between that hold nothing. Written as
     * CHILDREN those totals silently never compute, because the year's children
     * are trimesters and no day mark lives there.
     */
    DESCENDANTS,
    /**
     * One named period.
     */
    SPECIFIC
}
