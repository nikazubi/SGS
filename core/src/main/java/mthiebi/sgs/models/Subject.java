package mthiebi.sgs.models;

import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Objects;

@Entity(name = "SUBJECT")
@NoArgsConstructor
public class Subject extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    @Column(columnDefinition = "nvarchar(max)")
    private String teacher;

    public Subject(String name) {
        super();
        this.name = name;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

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

    @Override
    public boolean equals(Object o) {
        return this.id == ((Subject) o).getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
