package mthiebi.sgs.gradebook.service.roster;

import lombok.AllArgsConstructor;
import lombok.Data;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.StaffClassGrant;
import mthiebi.sgs.models.SystemUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Which classes a member of staff is limited to.
 * <p>
 * The only writer of {@code sgs.staff_class_grant}. Grants name class groups
 * by id, so a class created through the roster screen can be granted the
 * moment it exists.
 */
@Service
public class StaffClassGrantService {

    @PersistenceContext
    private EntityManager em;

    /**
     * The class groups this user is limited to. Empty means unrestricted.
     */
    @Transactional(readOnly = true)
    public Set<Long> classGroupIdsOf(Long systemUserId) {
        if (systemUserId == null) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(em.createQuery(
                        "select g.classGroup.id from StaffClassGrant g "
                                + "where g.systemUserId = :user", Long.class)
                .setParameter("user", systemUserId)
                .getResultList());
    }

    /**
     * The grants as the user form shows them. Includes past years, so a grant
     * left over from before a rollover is visible - and removable - rather than
     * silently narrowing the user to nothing.
     */
    @Transactional(readOnly = true)
    public List<GrantedClass> grantsOf(Long systemUserId) {
        return toRows(em.createQuery(
                        "select c from StaffClassGrant g join g.classGroup c "
                                + "join fetch c.school join fetch c.academicYear "
                                + "where g.systemUserId = :user "
                                + "order by c.academicYear.startsOn desc, c.school.ordinal, c.level, c.name",
                        ClassGroup.class)
                .setParameter("user", systemUserId)
                .getResultList());
    }

    /**
     * What the user form offers: every class of the current year.
     */
    @Transactional(readOnly = true)
    public List<GrantedClass> offerable() {
        return toRows(em.createQuery(
                        "select c from ClassGroup c join fetch c.school join fetch c.academicYear y "
                                + "where y.current = true "
                                + "order by c.school.ordinal, c.level, c.name", ClassGroup.class)
                .getResultList());
    }

    /**
     * Replaces the user's grant with exactly these class groups.
     */
    @Transactional(rollbackFor = Exception.class)
    public void replace(Long systemUserId, Collection<Long> classGroupIds) throws SGSException {
        SystemUser user = em.find(SystemUser.class, systemUserId);
        if (user == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "მომხმარებელი ვერ მოიძებნა");
        }
        Set<Long> wanted = classGroupIds == null
                ? Collections.emptySet() : new LinkedHashSet<>(classGroupIds);

        List<ClassGroup> classes = wanted.isEmpty() ? Collections.emptyList()
                : em.createQuery("select c from ClassGroup c where c.id in :ids", ClassGroup.class)
                .setParameter("ids", wanted)
                .getResultList();
        if (classes.size() != wanted.size()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "კლასი ვერ მოიძებნა");
        }

        em.createQuery("delete from StaffClassGrant g where g.systemUserId = :user")
                .setParameter("user", systemUserId)
                .executeUpdate();
        for (ClassGroup classGroup : classes) {
            StaffClassGrant grant = new StaffClassGrant();
            grant.setSystemUserId(systemUserId);
            grant.setClassGroup(classGroup);
            em.persist(grant);
        }
    }

    /**
     * Drops every grant naming this class, ahead of deleting it.
     */
    @Transactional(rollbackFor = Exception.class)
    public void revokeClass(Long classGroupId) {
        em.createQuery("delete from StaffClassGrant g where g.classGroup.id = :id")
                .setParameter("id", classGroupId)
                .executeUpdate();
    }

    private List<GrantedClass> toRows(List<ClassGroup> classes) {
        return classes.stream()
                .map(c -> new GrantedClass(c.getId(), c.getName(), c.getSchool().getName(),
                        c.getAcademicYear().getCode(), c.getAcademicYear().isCurrent()))
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    public static class GrantedClass {
        private Long id;
        private String name;
        private String schoolName;
        private String yearCode;
        private boolean currentYear;
    }
}
