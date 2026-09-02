package mthiebi.sgs.gradebook.service.grid;

import lombok.AllArgsConstructor;
import lombok.Data;
import mthiebi.sgs.gradebook.model.PeriodKind;

/**
 * One period of a scheme. The tree is sent flat with parent ids: it is a
 * handful of rows, and the console needs the whole shape anyway to offer
 * trimesters under a year.
 */
@Data
@AllArgsConstructor
public class PeriodOption {
    private Long id;
    private String code;
    private String label;
    private PeriodKind kind;
    private int depth;
    private int ordinal;
    private Long parentId;
}
