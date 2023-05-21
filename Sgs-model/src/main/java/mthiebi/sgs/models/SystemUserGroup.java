package mthiebi.sgs.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(indexes = {
        @Index(name = "nameIndex", columnList = "name", unique = true)
})
public class SystemUserGroup {

    private Long id;
    private String name;
    private String permissions;
    private Boolean active;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
