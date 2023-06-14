package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mthiebi.sgs.models.*;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRequestDTO {

    private Long id;

    private AcademyClassDTO academyClass;

    private StudentDTO student;

    private GradeDTO prevGrade;

    private String newValue;

}
