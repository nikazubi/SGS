package mthiebi.sgs.models;

import lombok.ToString;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@ToString
@Entity(name = "SYSTEM_USER_TABLE")
public class SystemUser extends Audit {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String username;

	private String password;

	private String name;

	private String email;

	private Boolean active;

	@ManyToMany(
			fetch = FetchType.EAGER,
			cascade = {CascadeType.ALL}
	)
	private List<SystemUserGroup> groups;

	@ManyToMany(
			fetch = FetchType.LAZY,
			cascade = {CascadeType.ALL}
	)
	private List<AcademyClass> academyClassList;

	public Long getId() {
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

	public List<SystemUserGroup> getGroups() {
		return groups;
	}

	public void setGroups(List<SystemUserGroup> groups) {
		this.groups = groups;
	}

	public List<AcademyClass> getAcademyClassList() {
		return academyClassList;
	}

	public void setAcademyClassList(List<AcademyClass> academyClassList) {
		this.academyClassList = academyClassList;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof SystemUser) {
			return Objects.equals(id, ((SystemUser) other).id);
		}
		return false;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getEmail() {
		return email;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}
}
