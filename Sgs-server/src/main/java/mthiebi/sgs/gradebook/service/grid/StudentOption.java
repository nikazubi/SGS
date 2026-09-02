package mthiebi.sgs.gradebook.service.grid;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * One student on a class list, for pickers.
 * <p>
 * Identified by enrollment rather than by student, because everything that
 * targets a child targets them in a class in a year - and a student id would
 * follow them into next year's class.
 */
@Data
@AllArgsConstructor
public class StudentOption {
    private Long enrollmentId;
    private Long studentId;
    /**
     * Surname first, as every list in this system shows it.
     */
    private String name;
}
