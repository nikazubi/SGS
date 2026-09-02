package mthiebi.sgs.gradebook.service.grid;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * What this user may do here, resolved once rather than guessed per cell.
 */
@Data
@AllArgsConstructor
public class GridCapabilities {
    private boolean canEdit;
    private boolean canOverride;
    private boolean canExplain;
}
