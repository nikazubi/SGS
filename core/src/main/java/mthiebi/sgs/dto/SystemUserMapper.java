package mthiebi.sgs.dto;

import mthiebi.sgs.models.SystemUser;
import org.mapstruct.Mapper;

@Mapper(config = ACMapperConfig.class)
public interface SystemUserMapper {

    SystemUser systemUser(SystemUserDTO systemUserDTO);

    SystemUserDTO systemUserDTO(SystemUser systemUser);

}
