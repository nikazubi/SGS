package mthiebi.sgs.models;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Entity(name = "TOTAL_ABSENCE")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TotalAbsence extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Date activePeriod;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)

    private AcademyClass academyClass;

    private Long totalAcademyHour;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getActivePeriod() {
        return activePeriod;
    }

    public void setActivePeriod(Date activePeriod) {
        this.activePeriod = activePeriod;
    }

    public AcademyClass getAcademyClass() {
        return academyClass;
    }

    public void setAcademyClass(AcademyClass academyClass) {
        this.academyClass = academyClass;
    }

    public Long getTotalAcademyHour() {
        return totalAcademyHour;
    }

    public void setTotalAcademyHour(Long totalAcademyHour) {
        this.totalAcademyHour = totalAcademyHour;
    }
}
