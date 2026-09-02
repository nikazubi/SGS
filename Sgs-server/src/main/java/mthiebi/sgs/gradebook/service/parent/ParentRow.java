package mthiebi.sgs.gradebook.service.parent;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One row of the parent view.
 * <p>
 * What a row *is* comes from the journal: a per-subject journal has one row per
 * subject for a chosen period, a class-wide one has a row per period. That is
 * also what decides how it is drawn - a single row reads as cards, several as a
 * table, because a one-row table is just an awkward way to show one thing.
 */
@Data
public class ParentRow {
    private String label;
    private Long subjectId;
    private Long periodId;
    /**
     * Column code to displayed value. Absent columns are still listed, empty.
     */
    private final Map<String, String> values = new LinkedHashMap<>();

    /**
     * The ceiling this row is judged against, where the journal has one.
     * <p>
     * Only the absence register sets it, from the permitted-missed-hours figure
     * the coordinator enters per month. It travels with the row because that is
     * the granularity it is stored at - October is red because October exceeded
     * October's allowance, not the year's.
     * <p>
     * Null everywhere else, and the chart simply does not colour.
     */
    private java.math.BigDecimal threshold;
}
