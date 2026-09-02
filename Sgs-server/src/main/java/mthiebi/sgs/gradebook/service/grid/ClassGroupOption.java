package mthiebi.sgs.gradebook.service.grid;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClassGroupOption {
    private Long id;
    private String name;
    private short level;
    private String schoolName;
    /**
     * So the console can pick periods without a second round trip.
     */
    private Long periodSchemeId;
}
