package mthiebi.sgs.controllers.gradebook;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * The child's name comes back so the portal can greet them without a second call.
 */
@Data
@AllArgsConstructor
public class ParentToken {
    private String token;
    private String firstName;
    private String lastName;
}
