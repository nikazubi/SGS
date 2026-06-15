package mthiebi.sgs.models;

import lombok.ToString;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ToString
@Entity(name = "SYSTEM_GROUPS")
public class SystemUserGroup extends Audit{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    private String permissions;

    private Boolean active;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPermissions() {
        return permissions;
    }

    @Transient
    public List<String> getListPermissions() {
        return Arrays.stream(permissions.split(",")).collect(Collectors.toList());
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

}
