package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mthiebi.sgs.gradebook.service.roster.StaffClassGrantService;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemUserDTO {

    private Long id;

    private String username;

    private String password;

    private String name;

    private String email;

    private Boolean active;

    /**
     * The classes this user is limited to - sgs.staff_class_grant. Filled by
     * the controller, not the mapper: the grant is not on the legacy entity.
     */
    private List<StaffClassGrantService.GrantedClass> classGroups;

    private List<SystemUserGroupDTO> groups;
}

