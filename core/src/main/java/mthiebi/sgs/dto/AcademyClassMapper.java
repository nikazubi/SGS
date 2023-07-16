package mthiebi.sgs.dto;

import mthiebi.sgs.models.AcademyClass;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = ACMapperConfig.class, imports = {StudentMapper.class, SubjectMapper.class})
public interface AcademyClassMapper {

    @Mapping(target = "isTransit", source = "isTransit")
    AcademyClassDTO academyClassDTO(AcademyClass academyClass);

    AcademyClass academyClass(AcademyClassDTO academyClassDTO);

}
