package mthiebi.sgs.models;

import javax.persistence.*;
import java.util.List;

@Entity(name = "ACADEMY_CLASS")
public class AcademyClass extends Audit{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long classLevel;

    private String classLevelIndex;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "academy_class_id")
    private List<Student> studentList;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "class_subject",
            joinColumns = @JoinColumn(name = "academy_class_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id"))
    private List<Subject> subjectList;

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

    public String getClassLevelIndex() {
        return classLevelIndex;
    }

    public void setClassLevelIndex(String classLevelIndex) {
        this.classLevelIndex = classLevelIndex;
    }

    public List<Student> getStudentList() {
        return studentList;
    }

    public void setStudentList(List<Student> studentList) {
        this.studentList = studentList;
    }

    public List<Subject> getSubjectList() {
        return subjectList;
    }

    public void setSubjectList(List<Subject> subjectList) {
        this.subjectList = subjectList;
    }
}
