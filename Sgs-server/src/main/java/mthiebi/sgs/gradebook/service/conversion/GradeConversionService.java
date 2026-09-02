package mthiebi.sgs.gradebook.service.conversion;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.ConversionFormula;
import mthiebi.sgs.gradebook.repository.ConversionFormulaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * The school grades on one scale and reports on another.
 * <p>
 * IB Mtiebi marks German-style, out of 7, but is legally required to report to
 * the government out of 10 - so the converted view is a compliance output, not
 * a convenience. Today that conversion is a hardcoded "+3" inside two
 * copy-pasted export methods, switched by a checkbox.
 * <p>
 * They are moving to a 9-point scale and the 9-to-10 mapping is not settled, so
 * the formula is configuration rather than code.
 * <p>
 * **Representation only.** Nothing is ever stored converted, nothing recomputes
 * through this, and the parent portal does not use it. It applies in exactly two
 * places - the grid when the toggle is on, and the Excel export when the box is
 * ticked - so changing the formula cannot corrupt a grade or require a
 * migration. The next render simply reads differently.
 * <p>
 * Named GradeConversionService rather than ConversionService, and not by taste:
 * Spring Boot binds @ConfigurationProperties through a bean it looks up by the
 * literal name "conversionService", expecting
 * org.springframework.core.convert.ConversionService. A @Service class with that
 * simple name takes the name first and every context in the application fails to
 * start - datasource binding included. Caught by ApplicationWiringIT.
 */
@Service
public class GradeConversionService {

    @Autowired
    private ConversionFormulaRepository conversionFormulaRepository;

    /**
     * The formula in force, or null when the school has not configured one.
     */
    @Transactional(readOnly = true)
    public ConversionFormula current() {
        List<ConversionFormula> all = conversionFormulaRepository.findAllByOrderByIdAsc();
        return all.isEmpty() ? null : all.get(0);
    }

    /**
     * Applies the formula, or returns null when there is nothing to apply.
     * <p>
     * Null means "print it as stored" and is distinct from a conversion that
     * produced a blank: callers fall back to the raw value on null.
     */
    public BigDecimal convert(BigDecimal raw, ConversionFormula formula) {
        if (raw == null || formula == null) {
            return null;
        }
        BigDecimal multiplier = formula.getMultiplier() == null
                ? BigDecimal.ONE : formula.getMultiplier();
        BigDecimal offset = formula.getOffsetValue() == null
                ? BigDecimal.ZERO : formula.getOffsetValue();

        // Deliberately not rounded. The engine rounded once when it calculated
        // the grade; a printed conversion has no business deciding it again, and
        // the school asked for the formula's output verbatim - 6.5 shows as 9.5.
        return raw.multiply(multiplier).add(offset);
    }

    /**
     * The printed form.
     * <p>
     * Trailing zeros are stripped, so a formula that turns 7 into 10.0000 prints
     * "10". toPlainString rather than toString because stripping can leave a
     * value in scientific notation - BigDecimal("10.00").stripTrailingZeros() is
     * 1E+1, which no one should ever be shown.
     */
    public String text(BigDecimal converted) {
        return converted == null ? "" : converted.stripTrailingZeros().toPlainString();
    }

    /**
     * Saves the one formula, creating it the first time.
     * <p>
     * rollbackFor is explicit because SGSException is checked, and Spring rolls
     * back on unchecked exceptions only - the defect a review found in three
     * other services, where a guard refused an operation and committed its
     * partial work anyway.
     */
    @Transactional(rollbackFor = Exception.class)
    public ConversionFormula save(String name, BigDecimal multiplier, BigDecimal offset)
            throws SGSException {

        if (name == null || name.trim().isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "შკალას სახელი სჭირდება");
        }
        if (multiplier != null && multiplier.signum() == 0) {
            // Every mark in the school would print as the offset. Always a slip.
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "გამრავლების კოეფიციენტი ნული ვერ იქნება");
        }

        ConversionFormula formula = current();
        if (formula == null) {
            formula = new ConversionFormula();
        }
        formula.setName(name.trim());
        formula.setMultiplier(multiplier);
        formula.setOffsetValue(offset);
        return conversionFormulaRepository.save(formula);
    }
}
