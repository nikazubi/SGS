package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.Subject;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeDTO {

    private Long id;

    private Long value;

    private String gradeType;

    private StudentDTO student;

    private SubjectDTO subject;

    private AcademyClassDTO academyClass;

}
