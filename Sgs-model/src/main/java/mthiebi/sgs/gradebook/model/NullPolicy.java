package mthiebi.sgs.gradebook.model;

/**
 * What an absent input does to a rule. Deliberately explicit: the old code did
 * all three inconsistently - counting present grades in one place and dividing
 * by a hardcoded 4 in another - so a student missing marks could be averaged
 * two different ways on two different screens.
 */
public enum NullPolicy {
    /**
     * Drop it; average over what is present.
     */
    IGNORE,
    /**
     * Treat as zero, which penalises missing work.
     */
    AS_ZERO,
    /**
     * Produce no value at all until every input is present.
     */
    BLOCK
}
