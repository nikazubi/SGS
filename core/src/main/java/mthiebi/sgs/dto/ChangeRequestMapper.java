package mthiebi.sgs.dto;

import mthiebi.sgs.models.ChangeRequest;
import mthiebi.sgs.models.ChangeRequestStatus;
import mthiebi.sgs.models.GradeType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = ACMapperConfig.class, imports = {StudentMapper.class,
                                                    AcademyClassMapper.class,
                                                    GradeMapper.class,
                                                    SubjectMapper.class})
public interface ChangeRequestMapper {

    ChangeRequest changeRequest(ChangeRequestDTO changeRequestDTO);

    @Mapping(source = "status", target = "status", qualifiedByName = "EnumTypeStatusToString")
    ChangeRequestDTO changeRequestDto(ChangeRequest changeRequest);

    @Named("EnumTypeStatusToString")
    default String stringToEnum(ChangeRequestStatus changeRequestStatus) {
        return changeRequestStatus.toString();
    }

}
