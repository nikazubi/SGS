package mthiebi.sgs.gradebook.engine;

import lombok.Value;
import mthiebi.sgs.gradebook.model.GradeSource;

import java.math.BigDecimal;

/**
 * A cell's current content. Absent cells are simply missing from the working
 * set rather than being represented by a null-valued instance, so "no mark yet"
 * and "a mark of zero" can never be confused.
 */
@Value
public class CellValue {

    BigDecimal value;
    String specialValue;
    GradeSource source;
    boolean override;
    int rowVersion;

    public static CellValue manual(BigDecimal value) {
        return new CellValue(value, null, GradeSource.MANUAL, false, 0);
    }

    public static CellValue special(String specialValue) {
        return new CellValue(null, specialValue, GradeSource.MANUAL, false, 0);
    }

    public static CellValue derived(BigDecimal value) {
        return new CellValue(value, null, GradeSource.DERIVED, false, 0);
    }

    public boolean isSpecial() {
        return specialValue != null;
    }

    /**
     * True when the cell holds neither a number nor a special code.
     */
    public boolean isEmpty() {
        return value == null && specialValue == null;
    }
}
