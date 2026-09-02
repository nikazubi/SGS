package mthiebi.sgs.gradebook.model;

public enum ComponentKind {
    /**
     * Typed in by a person.
     */
    INPUT,
    /**
     * Produced by the recompute engine from a DerivationRule.
     */
    DERIVED
}
