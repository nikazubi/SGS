package mthiebi.sgs.gradebook;

import mthiebi.sgs.controllers.gradebook.ClassScopeGuard;
import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.service.roster.StaffClassGrantService;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.utils.UtilsJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Where a member of staff may act, as opposed to what they may do.
 * <p>
 * A grant is a <em>narrowing</em>: holding none means unrestricted, which is
 * how "director" is expressed. Grants name sgs.class_group rows by id - the
 * legacy grant pointed at dbo.academy_class and was matched by name, so a
 * class created on the roster screen could never be granted to anybody.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ClassScopeGuard.class, StaffClassGrantService.class, UtilsJwt.class,
        QueryFactoryProvider.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none"
})
class ClassScopeIT {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ClassScopeGuard guard;

    @Autowired
    private StaffClassGrantService grants;

    @Autowired
    private UtilsJwt jwt;

    private GradebookTestData data;

    @BeforeEach
    void setUp() {
        data = new GradebookTestData(em).build(UUID.randomUUID().toString().substring(0, 8));
        em.flush();
    }

    // ---- what a listing may show --------------------------------------------

    @Test
    @DisplayName("a director sees the whole school")
    void unrestrictedUserIsNotNarrowed() throws Exception {
        String header = tokenFor(user("dir-"));

        assertFalse(guard.isRestricted(header));
        assertTrue(guard.visibleClassGroupIds(header).isEmpty(),
                "empty means unrestricted to every caller that filters with it");
    }

    @Test
    @DisplayName("a grant names the class group itself")
    void aGrantNamesTheClassGroup() throws Exception {
        SystemUser u = user("teach-");
        grants.replace(u.getId(), Collections.singletonList(data.classGroup.getId()));
        String header = tokenFor(u);

        assertTrue(guard.isRestricted(header));
        assertEquals(Collections.singleton(data.classGroup.getId()),
                guard.visibleClassGroupIds(header));
    }

    @Test
    @DisplayName("a class that exists only in sgs can be granted")
    void aClassWithNoLegacyTwinCanBeGranted() throws Exception {
        // The production failure: a class made on the roster screen has no
        // dbo.academy_class row, so the old name-matched grant could never
        // reach it and the user form could not even offer it.
        GradebookTestData other = new GradebookTestData(em)
                .build(UUID.randomUUID().toString().substring(0, 8));
        SystemUser u = user("new-");
        grants.replace(u.getId(), Collections.singletonList(other.classGroup.getId()));
        String header = tokenFor(u);

        assertEquals(Collections.singleton(other.classGroup.getId()),
                guard.visibleClassGroupIds(header));
        guard.check(header, other.classGroup.getId());
        assertTrue(grants.offerable().stream()
                        .anyMatch(c -> c.getId().equals(other.classGroup.getId())),
                "the user form offers it");
    }

    @Test
    @DisplayName("replacing a grant drops what it no longer names")
    void replacingAGrantDropsTheRest() throws Exception {
        SystemUser u = user("swap-");
        grants.replace(u.getId(), Collections.singletonList(data.classGroup.getId()));
        grants.replace(u.getId(), Collections.emptyList());

        assertFalse(guard.isRestricted(tokenFor(u)), "an emptied grant is no narrowing");
    }

    @Test
    @DisplayName("deleting a class revokes the grants that name it")
    void revokingAClassClearsItsGrants() throws Exception {
        SystemUser u = user("gone-");
        grants.replace(u.getId(), Collections.singletonList(data.classGroup.getId()));
        grants.revokeClass(data.classGroup.getId());

        assertTrue(grants.classGroupIdsOf(u.getId()).isEmpty());
    }

    @Test
    @DisplayName("acting on a class outside the grant is refused")
    void checkRefusesAClassOutsideTheGrant() throws Exception {
        SystemUser u = user("teach2-");
        grants.replace(u.getId(), Collections.singletonList(data.classGroup.getId()));
        String header = tokenFor(u);

        // The listing narrows; this is the other half, and it is the half that
        // matters when somebody posts an id the picker never offered them.
        guard.check(header, data.classGroup.getId());
        org.junit.jupiter.api.Assertions.assertThrows(mthiebi.sgs.SGSException.class,
                () -> guard.check(header, -99L));
    }

    // ---- fixture ------------------------------------------------------------

    private SystemUser user(String prefix) {
        SystemUser u = new SystemUser();
        u.setUsername(prefix + UUID.randomUUID().toString().substring(0, 8));
        u.setPassword("x");
        u.setActive(true);
        em.persist(u);
        em.flush();
        return u;
    }

    private String tokenFor(SystemUser user) {
        return "Bearer " + jwt.generateToken("", user.getUsername());
    }
}
