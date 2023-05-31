package mthiebi.sgs.components;

import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.models.SystemUserGroup;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserDetailsImpl implements UserDetails {

	private final SystemUser user;

	public UserDetailsImpl(SystemUser user) {
		super();
		this.user = user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		List<SimpleGrantedAuthority> authorities = new ArrayList<>();
		List<SystemUserGroup> userGroupList = user.getGroups();
		for (SystemUserGroup userGroup : userGroupList) {
			if (!userGroup.getActive()) {
				continue;
			}
			Stream<SimpleGrantedAuthority> s = Arrays.stream(userGroup.getPermissions().split(",")).map(SimpleGrantedAuthority::new);
			s.distinct().collect(Collectors.toCollection(() -> authorities));
		}
		if (authorities.size() == 0) {
			return Collections.emptyList();
		}
		return new HashSet<>(authorities);
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getUsername();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return user.getActive();
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return user.getActive();
	}
}
