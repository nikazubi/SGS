package mthiebi.sgs.gradebook.engine;

import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
public class TermTrace {
    String label;
    BigDecimal weight;
    BigDecimal reduced;
    List<SourceTrace> sources;
}
