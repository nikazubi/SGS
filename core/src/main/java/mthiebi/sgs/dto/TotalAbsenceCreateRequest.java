package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mthiebi.sgs.models.AcademyClass;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TotalAbsenceCreateRequest {

    private Date activePeriod;

    private List<AcademyClass> academyClasses;

    private Long totalAcademyHour;
}
