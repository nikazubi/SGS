package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mthiebi.sgs.models.*;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRequestDTO {

    private Long id;

    private GradeDTO prevGrade;

    private String issuerFullname;

    private BigDecimal prevValue;

    private BigDecimal newValue;

    private String status;

    private String description;
}
