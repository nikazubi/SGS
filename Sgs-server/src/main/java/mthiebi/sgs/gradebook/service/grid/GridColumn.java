package mthiebi.sgs.gradebook.service.grid;

import lombok.Data;
import mthiebi.sgs.gradebook.model.ComponentKind;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * One column, as configured.
 * <p>
 * The page this replaces declared eleven of these in JSX, so adding a column
 * meant a code change and a deployment. Everything here comes from the
 * template version.
 */
@Data
public class GridColumn {

    private String code;
    private String label;
    private int ordinal;
    private ComponentKind kind;

    /**
     * Set for the header group this column sits under, null if it stands alone.
     */
    private String groupLabel;

    private boolean editable;
    private boolean allowOverride;

    private int decimals;
    private BigDecimal scaleMin;
    private BigDecimal scaleMax;
    private boolean allowSpecialValues;

    /**
     * Which calculated columns change when this one does, and which columns
     * feed this one.
     * <p>
     * Static per template version and free to derive - the dependency graph is
     * already built - so the console can dim exactly the right cells while a
     * flush is in flight instead of dimming a whole row. This is a list of
     * edges, not evaluation logic: no part of the evaluator moves into the
     * browser, and the numbers shown are always the server's.
     */
    private final List<String> dependents = new ArrayList<>();
    private final List<String> dependsOn = new ArrayList<>();

    /**
     * Whether a conversion formula is configured at all.
     * <p>
     * The console shows the toggle only when one is. Per column rather than per
     * grid purely because the console already reads columns; there is one
     * formula for the school, so every column carries the same answer.
     */
    private boolean hasConversion;
}
