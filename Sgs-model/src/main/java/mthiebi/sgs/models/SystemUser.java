package mthiebi.sgs.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class SystemUser extends Audit {

	private Long id;
	private String username;
	private String password;
	private String name;
	private String email;
	private Boolean active;

	@Builder.Default
	private List<SystemUserGroup> groups = new ArrayList<>();


	@Id
	@GeneratedValue(generator = "SystemUserSequence", strategy = GenerationType.SEQUENCE)
	@SequenceGenerator(name = "SystemUserSequence", allocationSize = 5)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Column(length = 512)
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToMany(fetch = FetchType.EAGER)
	public List<SystemUserGroup> getGroups() {
		return groups;
	}

	public void setGroups(List<SystemUserGroup> groups) {
		this.groups = groups;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof SystemUser) {
			return Objects.equals(id, ((SystemUser) other).id);
		}
		return false;
	}
}
