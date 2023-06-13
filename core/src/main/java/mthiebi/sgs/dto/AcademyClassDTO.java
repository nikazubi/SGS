package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademyClassDTO {

    private Long id;

    private Long classLevel;

    private List<StudentDTO> studentList;

    private List<SubjectDTO> subjectList;

    private String className;

}
