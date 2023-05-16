package mthiebi.sgs.dto;

import mthiebi.sgs.models.AcademyClass;
import org.mapstruct.Mapper;

@Mapper
public interface AcademyClassMapper {

    AcademyClassDTO academyClassDTO(AcademyClass academyClass);

    AcademyClass academyClass(AcademyClassDTO academyClassDTO);

}
