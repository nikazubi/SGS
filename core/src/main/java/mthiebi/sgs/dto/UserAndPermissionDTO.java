package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAndPermissionDTO {

    private String username;

    private List<String> permissionList;

}