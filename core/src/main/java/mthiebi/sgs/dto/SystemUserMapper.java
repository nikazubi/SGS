package mthiebi.sgs.dto;

import mthiebi.sgs.models.SystemUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = ACMapperConfig.class, imports = {SystemUserGroupMapper.class})
public interface SystemUserMapper {

    SystemUser systemUser(SystemUserDTO systemUserDTO);

    @Mapping(target = "classGroups", ignore = true)
    SystemUserDTO systemUserDTO(SystemUser systemUser);
}
