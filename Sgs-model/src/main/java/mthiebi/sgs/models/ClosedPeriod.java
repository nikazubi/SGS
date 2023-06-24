package mthiebi.sgs.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity(name = "CLOSED_PERIOD")
public class ClosedPeriod extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long AcademyClassId;

    private String gradePrefix;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getAcademyClassId() {
        return AcademyClassId;
    }

    public void setAcademyClassId(Long academyClassId) {
        AcademyClassId = academyClassId;
    }

    public String getGradePrefix() {
        return gradePrefix;
    }

    public void setGradePrefix(String gradePrefix) {
        this.gradePrefix = gradePrefix;
    }
}
