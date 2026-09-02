package mthiebi.sgs.gradebook.service.absence;

import lombok.Data;
import mthiebi.sgs.gradebook.service.grid.GridStudent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Students down, periods across.
 * <p>
 * The transpose of the ordinary grid, which puts the template's columns across
 * and picks one period. Used by both absence journals: daily is a month of days,
 * monthly is a year of months.
 */
@Data
public class AbsenceGrid {

    /**
     * The period whose children are the columns - a month, or the year.
     */
    private Long parentPeriodId;
    private String parentPeriodLabel;

    /**
     * The single component being filled in, e.g. ABSENT or HOURS_MISSED.
     */
    private String componentCode;
    private String componentLabel;

    /**
     * Whether it is a tick-or-cross or a number, so the console knows what to draw.
     */
    private boolean toggle;

    private final List<AbsenceColumn> columns = new ArrayList<>();
    private final List<GridStudent> students = new ArrayList<>();
    private final List<AbsenceCell> cells = new ArrayList<>();

    /**
     * The two figures, per month.
     * <p>
     * A list, not a pair. They were a single pair carried for the whole grid,
     * read and written against the *year* - so setting September's 120 academic
     * hours silently set every month's. The brief has always said per class and
     * per month, and the permitted figure is what turns a parent's chart red,
     * so the granularity is the point rather than a detail.
     * <p>
     * Only months with something stored appear here.
     */
    private final List<PeriodSetting> settings = new ArrayList<>();

    /**
     * What this user may do here, as the ordinary grid also reports.
     */
    private boolean canEdit;

    /**
     * One column: a period, and the component whose value sits on it.
     * <p>
     * The pair is the point. The brief's absence and ethics tables put reporting
     * periods, trimester totals and the year in one row, and those are three
     * different components living on three kinds of period. A column that named
     * only its period could not say which value it carried.
     */
    @Data
    public static class AbsenceColumn {
        private final Long periodId;
        private final String code;
        private final String label;
        /**
         * Set for day columns; lets the console group by week or grey a weekend.
         */
        private final String date;
        private final String componentCode;
        private final String componentLabel;
        /**
         * 0 year, 1 trimester, 2 reporting period. Drives the heavier styling.
         */
        private final int depth;
        /**
         * Typed here, or computed from the columns to its left.
         */
        private final boolean editable;
        private final int decimals;
    }

    /**
     * One month's two numbers. Either may be null; the pair is not stored as one.
     */
    @Data
    public static class PeriodSetting {
        private final Long periodId;
        private final BigDecimal totalAcademicHours;
        private final BigDecimal permittedMissedHours;
    }

    @Data
    public static class AbsenceCell {
        private final Long id;
        private final Long enrollmentId;
        private final Long periodId;
        /**
         * Which column on that period. A period carried one component while
         * this grid was one level deep; the summary puts three trimester marks
         * and four year columns in one row, so the pair is the key.
         */
        private final String componentCode;
        private final BigDecimal value;
        private final int rowVersion;
        private final boolean published;
        private final BigDecimal publishedValue;
        private final boolean changedSincePublication;
        private final boolean changeRequestPending;
    }
}
