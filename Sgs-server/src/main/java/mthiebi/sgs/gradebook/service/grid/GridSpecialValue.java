package mthiebi.sgs.gradebook.service.grid;

import lombok.AllArgsConstructor;
import lombok.Data;
import mthiebi.sgs.gradebook.engine.SpecialValueBehaviour;

@Data
@AllArgsConstructor
public class GridSpecialValue {
    private String code;
    private String label;
    private SpecialValueBehaviour behaviour;
}
