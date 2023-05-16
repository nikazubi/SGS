package mthiebi.sgs.models;

import javax.persistence.*;
import java.util.List;

@Entity(name = "ACADEMY_CLASS")
public class AcademyClass {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long classLevel;

    private Long classLevelIndex;

    @OneToMany
    private List<Student> studentList;

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

    public Long getClassLevelIndex() {
        return classLevelIndex;
    }

    public void setClassLevelIndex(Long classLevelIndex) {
        this.classLevelIndex = classLevelIndex;
    }

    public List<Student> getStudentList() {
        return studentList;
    }

    public void setStudentList(List<Student> studentList) {
        this.studentList = studentList;
    }
}
