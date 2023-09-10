package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private List<AcademyClassDTO> academyClassList;

    private List<SystemUserGroupDTO> groups;
}

