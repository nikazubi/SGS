package mthiebi.sgs.dto;

import mthiebi.sgs.models.Subject;
import org.mapstruct.Mapper;

@Mapper
public interface SubjectMapper {

    SubjectDTO subjectDTO(Subject subject);

    Subject subject(SubjectDTO subjectDTO);

}
