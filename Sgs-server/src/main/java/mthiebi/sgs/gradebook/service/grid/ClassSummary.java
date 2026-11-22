package mthiebi.sgs.gradebook.service.grid;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One class, one period, every subject: the old monthly page, by trimester.
 * <p>
 * Students down; across, each subject's summary mark, then the class-wide
 * figures beside them - a rating, and whatever the class-wide journals hold for
 * the period (ethics, absence hours). Read-only, and built from the working
 * values, as the exports are.
 */
@Data
public class ClassSummary {

    private Long classGroupId;
    private String className;
    private Long periodId;
    private String periodLabel;
    private String journalUuid;
    private String journalName;

    /**
     * Whether a conversion formula is configured. Only columns that say they
     * convert carry a converted value.
     */
    private boolean convertible;

    private List<Column> columns = new ArrayList<>();
    private List<Row> rows = new ArrayList<>();
    private List<GridSpecialValue> specialValues = new ArrayList<>();

    public enum ColumnKind {
        /**
         * One subject's summary mark.
         */
        SUBJECT,
        /**
         * A class-wide column of the subject journal itself - the year's rating.
         */
        SUBJECT_JOURNAL,
        /**
         * Worked out here: the average of the row's subject marks.
         */
        AVERAGE,
        /**
         * Another journal's figure for the period - ethics, absence hours.
         */
        CLASS_JOURNAL
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Column {
        private String key;
        private String label;
        /**
         * What the label stands for, where it is not obvious - the column read.
         */
        private String detail;
        private ColumnKind kind;
        private Long subjectId;
        private String teacherName;
        private int decimals;
        /**
         * Put through the conversion formula when the 10-point scale is on.
         */
        private boolean converts;
    }

    @Data
    public static class Row {
        private Long enrollmentId;
        private Long studentId;
        private String firstName;
        private String lastName;
        private int index;
        /**
         * Keyed by {@link Column#getKey()}; a missing key is an empty cell.
         */
        private Map<String, Cell> cells = new LinkedHashMap<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cell {
        private BigDecimal value;
        private String specialValue;
        /**
         * The value on the printed scale, unrounded; null when nothing converts.
         */
        private BigDecimal convertedValue;
    }
}
