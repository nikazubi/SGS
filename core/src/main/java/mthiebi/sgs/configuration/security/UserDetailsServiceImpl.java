package mthiebi.sgs.configuration.security;


import mthiebi.sgs.components.UserDetailsImpl;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.repository.SystemUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	@Autowired
	private SystemUserRepository systemUserRepository;

	@Override
	public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {

		SystemUser user = systemUserRepository.findSystemUserByUsername(userName);
		if(user == null){
			throw new UsernameNotFoundException("User Not Found");
		}

		return new UserDetailsImpl(user);
	}
}
