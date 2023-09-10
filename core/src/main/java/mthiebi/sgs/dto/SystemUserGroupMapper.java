package mthiebi.sgs.dto;

import mthiebi.sgs.models.SystemUserGroup;
import org.mapstruct.Mapper;

@Mapper(config = ACMapperConfig.class)
public interface SystemUserGroupMapper {

    SystemUserGroup systemUserGroup(SystemUserGroupDTO systemUserGroupDTO);

    SystemUserGroupDTO systemUserGroupDTO(SystemUserGroup systemUserGroup);

}
