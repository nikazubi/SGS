package mthiebi.sgs.gradebook.service;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * What the flush did. The derived list is the important part: it carries the
 * recomputed values back so the client patches its own cache, instead of
 * invalidating and refetching the entire grid after every edit.
 */
@Data
public class GradeWriteResult {

    private final List<CellResult> applied = new ArrayList<>();
    private final List<CellResult> derived = new ArrayList<>();
    private final List<CellConflict> conflicts = new ArrayList<>();
}
