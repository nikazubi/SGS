package mthiebi.sgs.dto;

import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.SystemUser;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(config = ACMapperConfig.class, imports = {AcademyClassMapper.class, SystemUserGroupMapper.class})
public interface SystemUserMapper {

    SystemUser systemUser(SystemUserDTO systemUserDTO);

    SystemUserDTO systemUserDTO(SystemUser systemUser);

    @Mapping(target = "academyClassList", qualifiedByName = "list")
    SystemUserDTO systemUserDtoWithAcademyClasses(SystemUser systemUser);

    @Named("list")
    @IterableMapping(qualifiedByName = "academyClassDtoForSystemUser")
    List<AcademyClassDTO> academyClassDtoForSystemUser(List<AcademyClass> academyClass);

    @Named("academyClassDtoForSystemUser")
    @Mapping(target = "studentList", ignore = true)
    @Mapping(target = "subjectList", ignore = true)
    @Mapping(target = "isTransit", ignore = true)
    @Mapping(target = "classLevel", ignore = true)
    AcademyClassDTO academyClassDtoForSystemUser(AcademyClass academyClass);


}
