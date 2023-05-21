package mthiebi.sgs.dto;

import mthiebi.sgs.models.Student;
import org.mapstruct.Mapper;

@Mapper(config = ACMapperConfig.class)
public interface StudentMapper {

    StudentDTO studentDTO(Student student);

    Student student(StudentDTO studentDTO);

}
