package mthiebi.sgs.gradebook.service.absence;

import lombok.Data;
import mthiebi.sgs.gradebook.service.grid.GridStudent;

import java.util.ArrayList;
import java.util.List;

/**
 * A month of the daily register: students down, school days across.
 * <p>
 * The columns are dates rather than period ids. Nothing in this grid addresses
 * the period tree - a month is a pair of dates, and the school days in it are
 * the weekdays between them.
 */
@Data
public class DailyAbsenceGrid {

    /**
     * The month being shown. Still a period, because that is how a month is named.
     */
    private Long monthPeriodId;
    private String monthLabel;

    private final List<DayColumn> columns = new ArrayList<>();
    private final List<GridStudent> students = new ArrayList<>();

    /**
     * The absences. A cell not listed here is a child who was present.
     * <p>
     * No value travels with a cell because there is not one: the presence of the
     * entry is the whole of its meaning.
     */
    private final List<Mark> marks = new ArrayList<>();

    /**
     * Days absent this month, per enrollment - a count, not a stored rollup.
     */
    private final List<Total> totals = new ArrayList<>();

    private boolean canEdit;

    @Data
    public static class DayColumn {
        /**
         * ISO date; the console keys cells on this.
         */
        private final String date;
        /**
         * Day of the month, which is all the header needs to show.
         */
        private final int dayOfMonth;
        /**
         * MONDAY..FRIDAY, so the console can rule off the end of a week.
         */
        private final String dayOfWeek;
    }

    @Data
    public static class Mark {
        private final Long enrollmentId;
        private final String date;
    }

    @Data
    public static class Total {
        private final Long enrollmentId;
        private final long daysAbsent;
    }
}
