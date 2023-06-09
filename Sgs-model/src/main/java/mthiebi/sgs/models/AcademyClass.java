package mthiebi.sgs.models;

import javax.persistence.*;
import java.util.List;

@Entity(name = "ACADEMY_CLASS")
public class AcademyClass extends mthiebi.sgs.models.Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long classLevel;

    private String className;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "academy_class_id")
    private List<mthiebi.sgs.models.Student> studentList;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "class_subject",
            joinColumns = @JoinColumn(name = "academy_class_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id"))
    private List<mthiebi.sgs.models.Subject> subjectList;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getClassLevel() {
        return classLevel;
    }

    public void setClassLevel(Long classLevel) {
        this.classLevel = classLevel;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<mthiebi.sgs.models.Student> getStudentList() {
        return studentList;
    }

    public void setStudentList(List<mthiebi.sgs.models.Student> studentList) {
        this.studentList = studentList;
    }

    public List<mthiebi.sgs.models.Subject> getSubjectList() {
        return subjectList;
    }

    public void setSubjectList(List<mthiebi.sgs.models.Subject> subjectList) {
        this.subjectList = subjectList;
    }

    @Override
    public String toString() {
        return "AcademyClass{" +
                "id=" + id +
                ", classLevel=" + classLevel +
                ", className='" + className + '\'' +
                ", studentList=" + studentList +
                ", subjectList=" + subjectList +
                '}';
    }
}
