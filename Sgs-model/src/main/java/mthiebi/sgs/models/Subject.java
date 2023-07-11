package mthiebi.sgs.models;

import javax.persistence.*;
import java.util.List;

@Entity(name = "SUBJECT")
public class Subject extends Audit{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

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

}
