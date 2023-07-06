package mthiebi.sgs.models;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity(name = "CLOSED_PERIOD")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ClosedPeriod extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long academyClassId;

    private String gradePrefix;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getAcademyClassId() {
        return academyClassId;
    }

    public void setAcademyClassId(Long academyClassId) {
        this.academyClassId = academyClassId;
    }

    public String getGradePrefix() {
        return gradePrefix;
    }

    public void setGradePrefix(String gradePrefix) {
        this.gradePrefix = gradePrefix;
    }
}
