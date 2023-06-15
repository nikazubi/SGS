package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mthiebi.sgs.models.*;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRequestDTO {

    private Long id;

    private GradeDTO prevGrade;

    private String issuerFullname;

    private Long prevValue;

    private Long newValue;

    private String status;

}
