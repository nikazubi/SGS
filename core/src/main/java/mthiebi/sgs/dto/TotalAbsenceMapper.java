package mthiebi.sgs.dto;

import mthiebi.sgs.models.TotalAbsence;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = ACMapperConfig.class)
public interface TotalAbsenceMapper {

    TotalAbsence totalAbsence(TotalAbsenceDto totalAbsenceDto);

//    @Mapping(target = "academyClass.totalAbsences", ignore = true)
    TotalAbsenceDto totalAbsenceDto(TotalAbsence totalAbsence);
}
