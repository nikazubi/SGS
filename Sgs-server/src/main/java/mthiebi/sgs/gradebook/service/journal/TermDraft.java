package mthiebi.sgs.gradebook.service.journal;

import lombok.Data;
import mthiebi.sgs.gradebook.model.PeriodRef;
import mthiebi.sgs.gradebook.model.ReduceType;
import mthiebi.sgs.gradebook.model.SourceKind;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * One line of a formula: a weight and where the number comes from.
 */
@Data
public class TermDraft {
    private BigDecimal weight = BigDecimal.ONE;
    private SourceKind sourceKind = SourceKind.COMPONENT;
    private ReduceType reduce = ReduceType.FIRST_NON_NULL;
    /**
     * SAME when both journals are filled in equally often, which needs no
     * question. SPECIFIC when they are not, and the picker asks which
     * occurrence rather than the system inferring one.
     */
    private PeriodRef periodRef = PeriodRef.SAME;
    private Long periodId;
    private String label;
    private List<SourceDraft> sources = new ArrayList<>();
}
