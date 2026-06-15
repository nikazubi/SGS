package mthiebi.sgs.utils;

/**
 * Sentinel identifiers for the synthetic "pseudo-subject" rows that the grade dashboards expect.
 *
 * These are NOT real {@code Subject} rows in the database – they are fabricated on the fly and handed to the
 * frontend, which keys off these exact ids to render the behaviour / absence / rating columns. Changing the
 * values here without changing the admin-console and client-console frontends will break those views.
 */
public final class GradeViewConstants {

    private GradeViewConstants() {
    }

    /** Synthetic subject id for the per-student behaviour column. */
    public static final long BEHAVIOUR_SUBJECT_ID = 9999L;

    /** Synthetic subject id for the per-student absence column. */
    public static final long ABSENCE_SUBJECT_ID = 8888L;

    /** Synthetic subject id for the per-student rating column. */
    public static final long RATING_SUBJECT_ID = 7777L;
}
