package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.models.AcademyClass;
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
import java.util.stream.Collectors;

/**
 * Which classes this user may touch.
 * <p>
 * A permission says what someone may *do*; it has never said *where*. The
 * legacy system scoped staff by their `academyClassList`, and dropping that on
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

    @PersistenceContext
    private EntityManager em;

    /**
     * The legacy class ids this user is limited to, or empty when unrestricted.
     * <p>
     * Matched by name, because the new {@code sgs.class_group} rows were
     * migrated from {@code dbo.academy_class} and carry the same names within a
     * school; the legacy user tables still hold the grants.
     */
    public Set<Long> allowedClassGroupIds(String authHeader) throws SGSException {
        SystemUser user = userOf(authHeader);
        if (user == null) {
            return Collections.emptySet();
        }
        List<AcademyClass> granted = user.getAcademyClassList();
        if (granted == null || granted.isEmpty()) {
            return Collections.emptySet();
        }
        List<String> names = granted.stream()
                .map(AcademyClass::getClassName)
                .collect(Collectors.toList());

        return em.createQuery(
                        "select c.id from ClassGroup c where c.name in :names "
                                + "and c.academicYear.current = true", Long.class)
                .setParameter("names", names)
                .getResultList()
                .stream().collect(Collectors.toSet());
    }

    /**
     * The classes a listing may show this caller. Empty means unrestricted.
     * <p>
     * Use this, not {@link #allowedClassGroupIds}, wherever the answer filters a
     * list rather than checking one id. The difference is the whole point:
     * allowedClassGroupIds returns an empty set for *two* states - a director
     * with no narrowing, and a restricted user whose granted classes resolve to
     * nothing because one was renamed or the year rolled over. A filter that
     * reads empty as "unrestricted" therefore hands the second user the entire
     * school, which is the failure mode {@link #check} was deliberately built to
     * avoid.
     * <p>
     * The sentinel matches nothing, so a stale grant yields an empty listing.
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
     * Read from the grant itself rather than from the resolved ids, so a grant
     * that resolves to nothing still counts as restricted - otherwise a stale
     * grant would silently widen to the whole school.
     */
    public boolean isRestricted(String authHeader) throws SGSException {
        SystemUser user = userOf(authHeader);
        return user != null
                && user.getAcademyClassList() != null
                && !user.getAcademyClassList().isEmpty();
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
