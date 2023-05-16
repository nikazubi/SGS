package mthiebi.sgs.models;

import javax.persistence.*;
import java.util.List;

@Entity(name = "SUBJECT")
public class Subject extends Audit{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    @ManyToMany(mappedBy = "subjectList")
    private List<AcademyClass> academyClassList;

    // todo: შუალედური წერები და დამატებითი აქტივობების კონფიგურაცია

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AcademyClass> getAcademyClassList() {
        return academyClassList;
    }

    public void setAcademyClassList(List<AcademyClass> academyClassList) {
        this.academyClassList = academyClassList;
    }
}
