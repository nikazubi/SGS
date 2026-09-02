package mthiebi.sgs.gradebook.conversion;

import mthiebi.sgs.gradebook.model.ConversionFormula;
import mthiebi.sgs.gradebook.service.conversion.GradeConversionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The formula that turns a stored mark into the one the government is shown.
 * <p>
 * Pure: convert() takes the formula rather than looking it up, so nothing here
 * needs a database. Saving and loading it are covered in GradeExportServiceIT,
 * which has a real one.
 */
class GradeConversionServiceTest {

    private final GradeConversionService service = new GradeConversionService();

    @Test
    @DisplayName("the current 7-to-10 conversion is an offset")
    void sevenToTen() {
        // What the school runs on today: German-style marks out of 7, reported
        // to the government out of 10. The legacy code had the 3 written into
        // two copy-pasted export methods.
        assertEquals("10", text(new BigDecimal("7"), formula("1", "3")));
        assertEquals("7", text(new BigDecimal("4"), formula("1", "3")));
    }

    @Test
    @DisplayName("a 9-to-10 conversion is expressible as a ratio")
    void nineToTen() {
        // They are moving to a 9-point scale and have not settled the mapping.
        // A proportional one is multiplier 10/9, which is why the formula is not
        // just an offset.
        assertEquals("10", text(new BigDecimal("9"), formula("1.1111", "0.0001")));
    }

    @Test
    @DisplayName("a converted value is not rounded")
    void noRounding() {
        // The school asked for the formula's output verbatim. The legacy
        // exporter called longValue() here and turned 9.5 into 9.
        assertEquals("9.5", text(new BigDecimal("6.5"), formula("1", "3")));
    }

    @Test
    @DisplayName("trailing zeros are stripped rather than shown or exponentiated")
    void trailingZeros() {
        // BigDecimal("10.00").stripTrailingZeros() is 1E+1, which is why this
        // goes through toPlainString and not toString.
        assertEquals("10", text(new BigDecimal("7.00"), formula("1", "3")));
    }

    @Test
    @DisplayName("a null multiplier reads as one and a null offset as zero")
    void nullsAreIdentity() {
        assertEquals("7", text(new BigDecimal("7"), new ConversionFormula()));
    }

    @Test
    @DisplayName("null in, null out")
    void nullValue() {
        assertNull(service.convert(null, formula("1", "3")));
    }

    @Test
    @DisplayName("no formula means no conversion, which is not the same as a blank")
    void noFormula() {
        // Callers fall back to the stored value on null. An empty string here
        // would blank the cell instead.
        assertNull(service.convert(new BigDecimal("7"), null));
    }

    private String text(BigDecimal raw, ConversionFormula formula) {
        return service.text(service.convert(raw, formula));
    }

    private ConversionFormula formula(String multiplier, String offset) {
        ConversionFormula f = new ConversionFormula();
        f.setName("ათბალიანი");
        f.setMultiplier(new BigDecimal(multiplier));
        f.setOffsetValue(new BigDecimal(offset));
        return f;
    }
}
