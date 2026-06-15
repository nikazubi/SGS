package mthiebi.sgs.utils;

import mthiebi.sgs.models.GradeType;

import java.util.Calendar;
import java.util.Date;

/**
 * Single source of truth for the school's reporting-calendar rules. These rules look odd but are
 * intentional and are relied upon across the grade service and the grade repositories:
 *
 * <ul>
 *   <li><b>Reporting month</b> – February is reported under January, and October under September
 *       (the two months are merged into the preceding reporting month). Every other month maps to
 *       itself.</li>
 *   <li><b>Diagnostics</b> – diagnostic grades always roll into the semester-boundary months:
 *       DIAGNOSTICS_1/2 -&gt; December, DIAGNOSTICS_3/4 -&gt; June, regardless of the date they were
 *       entered on.</li>
 * </ul>
 *
 * Historically this logic was copy-pasted (in two slightly different shapes) into {@code GradeServiceImpl},
 * {@code GradeCalculationServiceImpl} and many {@code GradeRepositoryCustomImpl} queries. It is centralised
 * here so the behaviour stays consistent. Values must not be changed without changing the consuming
 * frontends.
 */
public final class GradePeriods {

    private GradePeriods() {
    }

    /** Zero-based reporting month for the given calendar (Feb -&gt; Jan, Oct -&gt; Sep, otherwise unchanged). */
    public static int reportingMonthZeroBased(Calendar calendar) {
        int month = calendar.get(Calendar.MONTH);
        if (month == Calendar.FEBRUARY) {
            return Calendar.JANUARY;
        }
        if (month == Calendar.OCTOBER) {
            return Calendar.SEPTEMBER;
        }
        return month;
    }

    /** Zero-based reporting month for the given date (Feb -&gt; Jan, Oct -&gt; Sep, otherwise unchanged). */
    public static int reportingMonthZeroBased(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return reportingMonthZeroBased(calendar);
    }

    /** Returns a copy of {@code date} whose month has been shifted to its reporting month (Feb -&gt; Jan, Oct -&gt; Sep). */
    public static Date normalizeToReportingMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MONTH, reportingMonthZeroBased(calendar));
        return calendar.getTime();
    }

    /**
     * Zero-based month a diagnostic grade should be stored/queried under: December for DIAGNOSTICS_1/2,
     * June for DIAGNOSTICS_3/4. For any other grade type the supplied {@code defaultZeroBasedMonth} is
     * returned unchanged.
     */
    public static int diagnosticMonthZeroBasedOrDefault(GradeType gradeType, int defaultZeroBasedMonth) {
        if (gradeType == GradeType.DIAGNOSTICS_1 || gradeType == GradeType.DIAGNOSTICS_2) {
            return Calendar.DECEMBER;
        }
        if (gradeType == GradeType.DIAGNOSTICS_3 || gradeType == GradeType.DIAGNOSTICS_4) {
            return Calendar.JUNE;
        }
        return defaultZeroBasedMonth;
    }
}
