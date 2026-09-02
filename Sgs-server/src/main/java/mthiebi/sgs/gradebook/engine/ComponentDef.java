package mthiebi.sgs.gradebook.engine;

import lombok.Value;
import mthiebi.sgs.gradebook.model.ComponentKind;
import mthiebi.sgs.gradebook.model.PeriodKind;

@Value
public class ComponentDef {
    Long id;
    String code;
    String label;
    int ordinal;
    ComponentKind kind;
    PeriodKind periodKind;
    boolean subjectScoped;
    boolean allowOverride;
    int decimals;
    /**
     * The range a typed value must fall in. Null means unbounded.
     */
    java.math.BigDecimal scaleMin;
    java.math.BigDecimal scaleMax;
    boolean allowSpecialValues;
    /**
     * Null for INPUT components.
     */
    RuleDef rule;

    public boolean isDerived() {
        return kind == ComponentKind.DERIVED && rule != null;
    }
}
