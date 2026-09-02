package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.gradebook.model.ConversionFormula;
import mthiebi.sgs.gradebook.service.conversion.GradeConversionService;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * The one formula marks are printed on.
 * <p>
 * Reading is open to anyone who may enter or manage marks, because the grid
 * needs to know whether to offer the toggle. Writing is behind MANAGE_TEMPLATES:
 * this is what the school reports to the government, and entering marks should
 * not imply the ability to change it.
 */
@RestController
@RequestMapping("/api/gradebook/conversion-formula")
public class ConversionFormulaController {

    @Autowired
    private GradeConversionService gradeConversionService;

    /**
     * Null when the school has not configured one, which the console reads as "no toggle".
     */
    @GetMapping
    @Secured({AuthConstants.ADD_GRADES, AuthConstants.MANAGE_GRADES,
            AuthConstants.MANAGE_TEMPLATES})
    public ConversionFormula get() {
        return gradeConversionService.current();
    }

    @PostMapping
    @Secured({AuthConstants.MANAGE_TEMPLATES})
    public ConversionFormula save(@RequestBody FormulaRequest request) throws SGSException {
        return gradeConversionService.save(request.getName(), request.getMultiplier(),
                request.getOffsetValue());
    }

    public static class FormulaRequest {

        private String name;
        private BigDecimal multiplier;
        private BigDecimal offsetValue;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(BigDecimal multiplier) {
            this.multiplier = multiplier;
        }

        public BigDecimal getOffsetValue() {
            return offsetValue;
        }

        public void setOffsetValue(BigDecimal offsetValue) {
            this.offsetValue = offsetValue;
        }
    }
}
