package mthiebi.sgs.models;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity(name = "ABSENCE_GRADES")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AbsenceGrade extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AbsenceGradeType gradeType;

    @Column(precision = 16, scale = 2)
    private BigDecimal value;

    @OneToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @OneToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @OneToOne
    @JoinColumn(name = "class_id")
    private AcademyClass academyClass;

    private Date exactMonth;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AbsenceGradeType getGradeType() {
        return gradeType;
    }

    public void setGradeType(AbsenceGradeType gradeType) {
        this.gradeType = gradeType;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public AcademyClass getAcademyClass() {
        return academyClass;
    }

    public void setAcademyClass(AcademyClass academyClass) {
        this.academyClass = academyClass;
    }

    public Date getExactMonth() {
        return exactMonth;
    }

    public void setExactMonth(Date exactMonth) {
        this.exactMonth = exactMonth;
    }
}
