package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.Student;
import mthiebi.sgs.utils.UtilsJwt;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Parent login.
 * <p>
 * Authenticates the (username, password) **pair**, which is what the school's
 * rule makes unique - a username on its own is not an identity here, because
 * two children may share one so long as their passwords differ.
 * <p>
 * The token is therefore keyed by student id, not by username. The legacy
 * endpoint put the username in the subject and looked the student up by it,
 * which with duplicate usernames silently serves the wrong child.
 * <p>
 * It also used QueryDSL {@code fetchOne()}, which throws when two rows match -
 * so six parents whose child had been entered twice with the same password
 * could not log in at all. The uniqueness constraint in db/015 is what makes a
 * single match guaranteed rather than hoped for.
 */
@RestController
@RequestMapping("/api/parent")
public class ParentAuthController {

    @Autowired
    private UtilsJwt utilsJwt;

    @PersistenceContext
    private EntityManager em;

    @PostMapping("/authenticate")
    public ParentToken authenticate(@RequestBody ParentCredentials credentials)
            throws SGSException {

        if (credentials.getUsername() == null || credentials.getPassword() == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "INVALID_CREDENTIALS");
        }

        // The stored form is the legacy one: unsalted MD5, uppercase hex. Kept
        // deliberately so that migrating did not lock 913 families out. It is
        // weak, and replacing it needs the school to agree a reset - see
        // CLIENT-QUESTIONS.md.
        String hashed = DigestUtils.md5Hex(credentials.getPassword()).toUpperCase();

        List<Student> found = em.createQuery(
                        "select s from Student s "
                                + "where s.username = :u and s.passwordHash = :p and s.active = true",
                        Student.class)
                .setParameter("u", credentials.getUsername())
                .setParameter("p", hashed)
                .getResultList();

        if (found.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "INVALID_CREDENTIALS");
        }
        if (found.size() > 1) {
            // uq_student_login should make this impossible. If it ever happens
            // the safe answer is to refuse, never to pick one - the cost of
            // guessing is showing a parent another family's child.
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "AMBIGUOUS_CREDENTIALS");
        }

        Student student = found.get(0);
        return new ParentToken(
                utilsJwt.generateTokenForStudent(String.valueOf(student.getId())),
                student.getFirstName(), student.getLastName());
    }
}
