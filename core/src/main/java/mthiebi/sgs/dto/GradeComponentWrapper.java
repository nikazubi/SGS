package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mthiebi.sgs.models.Student;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeComponentWrapper {

    private Student student;

    private List<SubjectComponentWrapper> gradeList;

}
