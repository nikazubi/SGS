package mthiebi.sgs.gradebook.engine;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Every cell relevant to one recompute, loaded up front in a single query.
 * <p>
 * The engine reads and writes only here, so evaluation performs no database
 * access at all. That is also why the write path needs no upsert: having loaded
 * the working set we already know which cells exist, so the flush splits
 * cleanly into a batched insert and a batched update.
 */
public class WorkingSet {

    private final Map<CellKey, CellValue> cells;
    private final Map<CellKey, CellValue> changed = new LinkedHashMap<>();
    private final Set<CellKey> preexisting;

    public WorkingSet(Map<CellKey, CellValue> initial) {
        this.cells = new HashMap<>(initial);
        this.preexisting = new java.util.HashSet<>(initial.keySet());
    }

    public static WorkingSet empty() {
        return new WorkingSet(new HashMap<>());
    }

    public Optional<CellValue> get(CellKey key) {
        return Optional.ofNullable(cells.get(key));
    }

    /**
     * Records a change only when the value actually differs, so an edit does
     * not rewrite half a class and updated_at stays meaningful.
     */
    public boolean put(CellKey key, CellValue value) {
        CellValue current = cells.get(key);
        if (current != null && sameContent(current, value)) {
            return false;
        }
        cells.put(key, value);
        changed.put(key, value);
        return true;
    }

    private boolean sameContent(CellValue a, CellValue b) {
        boolean sameNumber = a.getValue() == null
                ? b.getValue() == null
                : b.getValue() != null && a.getValue().compareTo(b.getValue()) == 0;
        boolean sameSpecial = java.util.Objects.equals(a.getSpecialValue(), b.getSpecialValue());
        return sameNumber && sameSpecial && a.isOverride() == b.isOverride();
    }

    /**
     * Cells written during this recompute, in the order they were written.
     */
    public Map<CellKey, CellValue> changed() {
        return java.util.Collections.unmodifiableMap(changed);
    }

    public boolean existedBefore(CellKey key) {
        return preexisting.contains(key);
    }

    public Set<CellKey> keys() {
        return java.util.Collections.unmodifiableSet(cells.keySet());
    }
}
