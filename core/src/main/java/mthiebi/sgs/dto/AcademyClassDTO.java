package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademyClassDTO {

    private Long classLevel;

    private String classLevelIndex;

    private List<StudentDTO> studentList;

    private List<SubjectDTO> subjectList;

}
