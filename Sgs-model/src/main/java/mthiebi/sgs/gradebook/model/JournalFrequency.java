package mthiebi.sgs.gradebook.model;

/**
 * How often a journal is filled in.
 * <p>
 * The only question the wizard asks about time, and the reason periods still
 * exist at all: a period is the same columns again for a different slice of the
 * year. Flattening a weekly journal into plain columns would need roughly 279
 * of them, and one journal per month would mean configuring nine near-identical
 * journals every year.
 * <p>
 * ONCE_A_YEAR is the default and behaves like a plain table: one grid, and no
 * period dropdown anywhere in the UI.
 * <p>
 * The value maps to a depth in the period tree, which is seeded consistently:
 * year at 0, trimesters at 1, months at 2, days at 3.
 * <p>
 * DAY is currently unused. Depth 3 held numbered weeks, which nothing ever
 * touched; phase 10 replaced them with dated days for the daily register, and
 * the revision that moved daily absence into its own table - keyed on a date
 * rather than a period - left the level empty again. Kept because the engine
 * still resolves a reach to any depth and a future journal may want one; the
 * seeded scheme simply stops at months.
 */
public enum JournalFrequency {

    ONCE_A_YEAR(0),
    TRIMESTER(1),
    MONTH(2),
    DAY(3);

    private final int depth;

    JournalFrequency(int depth) {
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }
}
