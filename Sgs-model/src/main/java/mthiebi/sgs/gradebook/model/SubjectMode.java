package mthiebi.sgs.gradebook.model;

/**
 * Whether a parent view opens on one subject or on all of them.
 * <p>
 * The difference the school's first two screens turn on: the same journal, the
 * same trimester, drawn once per subject and once across them. JournalPage
 * already renders both - a single row becomes cards, several become a table -
 * so this only decides which it is handed.
 * <p>
 * Meaningless for a journal that is not subject-scoped; such a journal has one
 * row per period whatever this says, and ALL is what those are seeded with.
 */
public enum SubjectMode {
    /**
     * One subject at a time, the parent choosing which. Drawn as cards.
     */
    ONE,
    /**
     * Every subject the class takes, as a table.
     */
    ALL
}
