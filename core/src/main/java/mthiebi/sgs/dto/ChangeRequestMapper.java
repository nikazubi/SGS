package mthiebi.sgs.dto;

import mthiebi.sgs.models.ChangeRequest;
import org.mapstruct.Mapper;

@Mapper(config = ACMapperConfig.class, imports = {StudentMapper.class,
                                                    AcademyClassMapper.class,
                                                    GradeMapper.class,
                                                    SubjectMapper.class})
public interface ChangeRequestMapper {

    ChangeRequest changeRequest(ChangeRequestDTO changeRequestDTO);

    ChangeRequestDTO changeRequestDto(ChangeRequest changeRequest);

}
