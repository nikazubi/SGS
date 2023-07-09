package mthiebi.sgs.dto;

import mthiebi.sgs.models.Grade;
import mthiebi.sgs.models.GradeType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = ACMapperConfig.class, imports = {StudentMapper.class,
                                                    AcademyClassMapper.class,
                                                    SubjectMapper.class,
                                                    AuditMapper.class})
public interface GradeMapper {

    @Mapping(source = "gradeType", target = "gradeType", qualifiedByName = "stringGradeTypeToEnum")
    Grade grade(GradeDTO gradeDTO);

    GradeDTO gradeDTO(Grade grade);

    @Named("stringGradeTypeToEnum")
    default GradeType stringToEnum(String gradeType) {
        return GradeType.valueOf(gradeType);
    }

}
