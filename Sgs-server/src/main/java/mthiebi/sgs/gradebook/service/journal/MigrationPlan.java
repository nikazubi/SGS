package mthiebi.sgs.gradebook.service.journal;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What migrating would do, in the words the prompt uses.
 * <p>
 * "412 cells across 3 subjects will be recalculated, and 24 marks in 2 removed
 * columns will be deleted." The second sentence is the one that should make
 * someone stop, which is why removed columns are named rather than counted.
 */
@Data
public class MigrationPlan {
    private String journalName;
    private int targetVersionNo;
    private final List<MigrationScope> scopes = new ArrayList<>();
    private int cellsToRecalculate;
    private int marksToDelete;
    private final Set<String> removedColumns = new LinkedHashSet<>();
    /**
     * False for a preview.
     */
    private boolean applied;
}
