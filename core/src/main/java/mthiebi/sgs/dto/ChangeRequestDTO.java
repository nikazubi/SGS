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

    private StudentDTO student;

    private GradeDTO prevGrade;

    private SubjectDTO subject;

    private Long prevValue;

    private Long newValue;

    private String status;

}
