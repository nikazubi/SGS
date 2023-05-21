package mthiebi.sgs.dto;

import mthiebi.sgs.models.Subject;
import org.mapstruct.Mapper;

@Mapper(config = ACMapperConfig.class)
public interface SubjectMapper {

    SubjectDTO subjectDTO(Subject subject);

    Subject subject(SubjectDTO subjectDTO);

}
