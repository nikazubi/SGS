package mthiebi.sgs.gradebook.service.grid;

import lombok.AllArgsConstructor;
import lombok.Data;
import mthiebi.sgs.gradebook.model.PeriodKind;

@Data
@AllArgsConstructor
public class GridPeriod {
    private Long id;
    private String code;
    private String label;
    private PeriodKind kind;
}
