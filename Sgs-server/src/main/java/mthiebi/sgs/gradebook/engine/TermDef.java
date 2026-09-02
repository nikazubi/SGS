package mthiebi.sgs.gradebook.engine;

import lombok.Value;
import mthiebi.sgs.gradebook.model.PeriodRef;
import mthiebi.sgs.gradebook.model.ReduceType;
import mthiebi.sgs.gradebook.model.SourceKind;

import java.math.BigDecimal;
import java.util.List;

/**
 * One weighted contribution to a rule, flattened out of the entity graph.
 */
@Value
public class TermDef {
    int ordinal;
    BigDecimal weight;
    SourceKind sourceKind;
    ReduceType reduce;
    PeriodRef periodRef;
    Long specificPeriodId;
    List<Long> sourceComponentIds;
    String label;
}
