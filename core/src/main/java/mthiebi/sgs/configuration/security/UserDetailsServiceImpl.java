package mthiebi.sgs.configuration.security;


import mthiebi.sgs.components.UserDetailImplStudent;
import mthiebi.sgs.components.UserDetailsImpl;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.repository.StudentRepository;
import mthiebi.sgs.repository.SystemUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @javax.persistence.PersistenceContext
    private javax.persistence.EntityManager em;


	@Autowired
	private SystemUserRepository systemUserRepository;

	@Autowired
	private StudentRepository studentRepository;

	@Override
	public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {

		SystemUser user = systemUserRepository.findSystemUserByUsername(userName);
		if(user == null){
			throw new UsernameNotFoundException("User Not Found");
		}

		return new UserDetailsImpl(user);
	}

	public UserDetails loadUserByUsername(String username, HttpServletRequest httpServletRequest) {
        // The parent portal authenticates a student, not a member of staff.
        // /api/parent/ is deliberately not in the permitAll list the way
        // /client/ is - a portal that serves a child's grades has to require a
        // token, and the student it belongs to is who the request is answered
        // for.
        String url = httpServletRequest.getRequestURL().toString();
        if (url.contains("/api/parent/")) {
            // The subject is the student's id: a username is not unique here,
            // so a token keyed by one could resolve to the wrong child.
            return parentUserDetails(username);
        }
        if (url.contains("/client/")) {
			Student student = studentRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User Not Found"));
			return new UserDetailImplStudent(student);
		} else {
			return loadUserByUsername(username);
		}
	}

    private UserDetails parentUserDetails(String studentId) {
        mthiebi.sgs.gradebook.model.Student student;
        try {
            student = em.find(mthiebi.sgs.gradebook.model.Student.class, Long.valueOf(studentId));
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("User Not Found");
        }
        if (student == null || !student.isActive()) {
            throw new UsernameNotFoundException("User Not Found");
        }
        return new org.springframework.security.core.userdetails.User(
                studentId, "", java.util.Collections.emptyList());
    }
}
