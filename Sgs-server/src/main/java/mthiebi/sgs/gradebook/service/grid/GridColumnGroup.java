package mthiebi.sgs.gradebook.service.grid;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GridColumnGroup {
    private String label;
    private List<String> componentCodes;
}
