package mthiebi.sgs.gradebook.engine;

import lombok.Value;
import mthiebi.sgs.gradebook.model.PeriodKind;

import java.util.List;

@Value
public class PeriodNode {
    Long id;
    Long parentId;
    String code;
    int depth;
    PeriodKind kind;
    List<Long> childIds;
}
