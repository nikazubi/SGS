package mthiebi.sgs.dto;

import mthiebi.sgs.models.AbsenceGrade;
import mthiebi.sgs.models.AbsenceGradeType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = ACMapperConfig.class, imports = {StudentMapper.class,
        AcademyClassMapper.class,
        SubjectMapper.class,
        AuditMapper.class})
public interface AbsenceGradeMapper {

    @Mapping(source = "gradeType", target = "gradeType", qualifiedByName = "stringGradeTypeToEnum")
    AbsenceGrade absenceGrade(AbsenceGradeDto gradeDTO);

    AbsenceGradeDto absenceGradeDTO(AbsenceGrade grade);

    @Mapping(target = "academyClass", ignore = true)
    AbsenceGradeDto absenceGradeDTOWithoutAcademyClass(AbsenceGrade grade);

    @Named("stringGradeTypeToEnum")
    default AbsenceGradeType stringToEnum(String gradeType) {
        return AbsenceGradeType.valueOf(gradeType);
    }
}
