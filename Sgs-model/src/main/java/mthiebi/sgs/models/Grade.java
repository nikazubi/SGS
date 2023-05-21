package mthiebi.sgs.models;

import javax.persistence.*;

@Entity(name = "GRADES")
public class Grade extends Audit{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    private GradeType gradeType;

    private Long value;

    @OneToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @OneToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @OneToOne
    @JoinColumn(name = "class_id")
    private AcademyClass academyClass;


    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public GradeType getGradeType() {
        return gradeType;
    }

    public void setGradeType(GradeType gradeType) {
        this.gradeType = gradeType;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
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
}
