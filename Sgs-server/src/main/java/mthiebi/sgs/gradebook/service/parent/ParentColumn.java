package mthiebi.sgs.gradebook.service.parent;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A column as a parent sees it.
 */
@Data
@AllArgsConstructor
public class ParentColumn {
    private String code;
    private String label;
    private String groupLabel;
    private int decimals;
}
