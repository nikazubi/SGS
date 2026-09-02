package mthiebi.sgs.gradebook.service.grid;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubjectOption {
    private Long id;
    private String name;
    private String shortName;
    /**
     * Who teaches it to this class. Shown in the grid header, as the old page
     * did - and occasionally two names, because some subjects are co-taught.
     */
    private String teacherName;
}
