package mthiebi.sgs.gradebook.service.grid;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GridStudent {
    private Long enrollmentId;
    private Long studentId;
    private String firstName;
    private String lastName;
    /**
     * Display position, so the console stops recomputing it on every fetch.
     */
    private int index;
}
