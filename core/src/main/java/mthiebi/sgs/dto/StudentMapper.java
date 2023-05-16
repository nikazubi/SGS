package mthiebi.sgs.dto;

import mthiebi.sgs.models.Student;
import org.mapstruct.Mapper;

@Mapper
public interface StudentMapper {

    StudentDTO studentDTO(Student student);

    Student student(StudentDTO studentDTO);

}
