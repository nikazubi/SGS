package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mthiebi.sgs.models.ChangeRequestStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRequestStatusChangeDTO {

    private Long changeRequestId;

    private ChangeRequestStatus changeRequestStatus;

    private String description;

}
