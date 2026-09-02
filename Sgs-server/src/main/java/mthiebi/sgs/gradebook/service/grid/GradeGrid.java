package mthiebi.sgs.gradebook.service.grid;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * A whole grade entry screen in one response.
 * <p>
 * The old console needed a grid fetch, a class fetch and a subject fetch to
 * draw this, then refetched the grid after every single cell edit. Everything
 * the screen needs to render and to write safely is here.
 */
@Data
public class GradeGrid {

    private GridPeriod period;
    private GridTemplateVersion templateVersion;

    /**
     * Header groups, e.g. the seven ongoing marks under "მიმდინარე შეფასება".
     */
    private final List<GridColumnGroup> columnGroups = new ArrayList<>();

    private final List<GridColumn> columns = new ArrayList<>();

    /**
     * Non-numeric marks the columns accept, e.g. ჩთ.
     */
    private final List<GridSpecialValue> specialValues = new ArrayList<>();

    private final List<GridStudent> students = new ArrayList<>();

    /**
     * Flat, not nested inside students. The client indexes it once by
     * (enrollment, component); the old page ran a linear scan of the student's
     * grade array inside renderCell, on every render of every cell.
     */
    private final List<GridCell> cells = new ArrayList<>();

    private GridCapabilities capabilities;
}
