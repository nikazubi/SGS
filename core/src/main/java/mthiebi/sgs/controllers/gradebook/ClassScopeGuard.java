package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.service.roster.StaffClassGrantService;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.repository.SystemUserRepository;
import mthiebi.sgs.utils.UtilsJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Which classes this user may touch.
 * <p>
 * A permission says what someone may *do*; it has never said *where*. The
 * legacy system scoped staff by their `academyClassList` - now
 * {@code sgs.staff_class_grant} - and dropping that on
 * the way to the new endpoints turned every class-level permission into a
 * school-wide one - any teacher who may enter grades could enter them for any
 * class, and anyone who may publish could publish the whole school.
 * <p>
 * A user with no classes assigned is unrestricted, which is how the legacy data
 * expresses "director" - the grant is a narrowing, not a requirement.
 */
@Component
public class ClassScopeGuard {

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Autowired
    private UtilsJwt utilsJwt;

    @Autowired
    private StaffClassGrantService grants;

    @PersistenceContext
    private EntityManager em;

    /**
     * The class groups this user is limited to, or empty when unrestricted.
     * <p>
     * Read from {@code sgs.staff_class_grant}, which names class groups by id.
     * This used to translate the legacy {@code dbo} grant by class name, which
     * could never include a class created through the roster screen - those
     * exist only in {@code sgs}.
     */
    public Set<Long> allowedClassGroupIds(String authHeader) throws SGSException {
        SystemUser user = userOf(authHeader);
        if (user == null) {
            return Collections.emptySet();
        }
        return grants.classGroupIdsOf(user.getId());
    }

    /**
     * The classes a listing may show this caller. Empty means unrestricted.
     * <p>
     * Use this, not {@link #allowedClassGroupIds}, wherever the answer filters a
     * list rather than checking one id. Grants are by id now, so the two only
     * disagree if isRestricted and allowedClassGroupIds ever stop reading the
     * same rows - and if they do, the sentinel keeps a restricted user from
     * reading an empty set as the entire school.
     * <p>
     * The sentinel matches nothing. A grant left over from last year is handled
     * by the listings themselves, which only offer current-year classes.
     */
    public Set<Long> visibleClassGroupIds(String authHeader) throws SGSException {
        if (!isRestricted(authHeader)) {
            return Collections.emptySet();
        }
        Set<Long> allowed = allowedClassGroupIds(authHeader);
        return allowed.isEmpty() ? Collections.singleton(NOTHING) : allowed;
    }

    /**
     * No class group has a negative id, so this narrows every listing to nothing.
     */
    private static final Long NOTHING = -1L;

    /**
     * Throws unless the caller may act on this class.
     */
    public void check(String authHeader, Long classGroupId) throws SGSException {
        if (classGroupId == null) {
            return;
        }
        if (!isRestricted(authHeader)) {
            return;
        }
        // Fails closed. A restricted user whose granted classes match nothing -
        // because a class was renamed, or the year rolled over - used to get an
        // empty set, and an empty set read as unrestricted. The failure mode of
        // a scope check must not be full access to the school.
        if (!allowedClassGroupIds(authHeader).contains(classGroupId)) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "ამ კლასზე წვდომა არ გაქვთ");
        }
    }

    /**
     * The class a cell belongs to, for endpoints addressed by grade_entry.
     */
    public void checkCell(String authHeader, Long gradeEntryId) throws SGSException {
        if (gradeEntryId == null || !isRestricted(authHeader)) {
            return;
        }
        List<Long> classIds = em.createQuery(
                        "select g.enrollment.classGroup.id from GradeEntry g where g.id = :id", Long.class)
                .setParameter("id", gradeEntryId).getResultList();
        if (!classIds.isEmpty()) {
            check(authHeader, classIds.get(0));
        }
    }

    /**
     * The class a cell belongs to, for endpoints addressed by enrollment.
     */
    public void checkEnrollment(String authHeader, Long enrollmentId) throws SGSException {
        if (enrollmentId == null || !isRestricted(authHeader)) {
            return;
        }
        List<Long> classIds = em.createQuery(
                        "select e.classGroup.id from Enrollment e where e.id = :id", Long.class)
                .setParameter("id", enrollmentId).getResultList();
        if (!classIds.isEmpty()) {
            check(authHeader, classIds.get(0));
        }
    }

    /**
     * Whether a narrowing applies at all.
     * <p>
     * Any grant row counts, whatever year its class is in, so a grant left
     * over from last year still narrows - to nothing current - rather than
     * silently widening to the whole school.
     */
    public boolean isRestricted(String authHeader) throws SGSException {
        SystemUser user = userOf(authHeader);
        return user != null && !grants.classGroupIdsOf(user.getId()).isEmpty();
    }

    private SystemUser userOf(String authHeader) {
        try {
            return systemUserRepository
                    .findSystemUserByUsername(utilsJwt.getUsernameFromHeader(authHeader));
        } catch (Exception e) {
            return null;
        }
    }
}
