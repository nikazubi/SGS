package mthiebi.sgs.gradebook.engine;

/**
 * What a special code such as ჩთ does inside an aggregation. Declared per code
 * on the template rather than assumed, because the old system treated its -50
 * sentinel as text on some screens and averaged it as a number on others.
 */
public enum SpecialValueBehaviour {
    /**
     * Not counted at all.
     */
    EXCLUDE,
    /**
     * Counted as zero.
     */
    AS_ZERO,
    /**
     * The dependent value cannot be produced.
     */
    BLOCK
}
