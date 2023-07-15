package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mthiebi.sgs.models.AcademyClass;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TotalAbsenceDto {
    private Long id;

    private Date activePeriod;

    private AcademyClassDTO academyClass;

    private Long totalAcademyHour;
}
