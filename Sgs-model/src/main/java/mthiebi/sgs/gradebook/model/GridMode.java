package mthiebi.sgs.gradebook.model;

/**
 * Which way round a journal's grid is drawn.
 * <p>
 * Almost every journal is COMPONENTS: students down, the template's columns
 * across, one period chosen from a dropdown.
 * <p>
 * The monthly absence register is the transpose - students down, *periods*
 * across, one column: a year of months, each holding the academic hours missed.
 * <p>
 * It was two journals when this was added, daily and monthly, which is why it is
 * a journal property rather than a hardcoded screen. Daily absence has since
 * moved to its own table and is no longer a journal at all, so PERIODS currently
 * has one user. The property earns its place anyway: the alternative is a screen
 * that knows a journal by name.
 * <p>
 * Only the read differs. Writes, recomputation and publication are addressed by
 * cell coordinates and have no opinion about layout.
 */
public enum GridMode {

    /**
     * Students down, template columns across.
     */
    COMPONENTS,

    /**
     * Students down, the chosen period's children across.
     */
    PERIODS
}
