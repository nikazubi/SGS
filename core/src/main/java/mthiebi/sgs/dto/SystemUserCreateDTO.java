package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemUserCreateDTO {

    private SystemUserDTO systemUserDTO;

    private List<Long> groupIdList;

    private List<Long> classIdList;

}
