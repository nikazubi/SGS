package mthiebi.sgs.gradebook;

import mthiebi.sgs.controllers.gradebook.ClassScopeGuard;
import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.repository.AcademyClassRepository;
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
 * Two rules, and they have to agree. A grant is a <em>narrowing</em>: holding
 * none means unrestricted, which is how the legacy data expresses "director".
 * A grant that no longer resolves is the opposite - still a narrowing, to
 * nothing - because a class renamed or a year rolled over must not silently
 * hand somebody the whole school.
 * <p>
 * The two used to disagree. {@code ClassScopeGuard} read an empty grant as
 * unrestricted while the legacy class list read it as "nothing", and because
 * grants only ever filled as a side effect of creating a class through the
 * console, a school whose classes arrived by migration had nobody holding one.
 * The class field on the user form was then empty for the very administrator
 * who needed to grant classes with it, so no user could be restricted at all.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ClassScopeGuard.class, UtilsJwt.class, QueryFactoryProvider.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none"
})
class ClassScopeIT {

    @PersistenceContext
    private EntityManager em;

    // The repository rather than the custom impl: Spring Data wires the
    // fragment itself, and importing it as well registers it twice.
    @Autowired
    private AcademyClassRepository classes;

    @Autowired
    private ClassScopeGuard guard;

    @Autowired
    private UtilsJwt jwt;

    private GradebookTestData data;
    private AcademyClass mine;
    private AcademyClass theirs;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        data = new GradebookTestData(em).build(suffix);

        // The guard matches the legacy grant to the new class group by name,
        // so the fixture's class group is the one this grant has to resolve to.
        mine = academyClass(data.classGroup.getName());
        theirs = academyClass("10B-" + suffix);
        em.flush();
    }

    // ---- the list the user form offers --------------------------------------

    @Test
    @DisplayName("a user holding no grant is offered every class")
    void anEmptyGrantIsNotARestriction() {
        List<AcademyClass> offered = classes.getAcademyClasses(new ArrayList<>(), null);

        // The chicken and egg this fixes: whoever grants classes has to be able
        // to see them, and an administrator is exactly the person least likely
        // to be tied to a class of their own.
        assertTrue(contains(offered, mine) && contains(offered, theirs),
                "an empty grant means no narrowing, not nothing");
    }

    @Test
    @DisplayName("a null grant is treated the same as an empty one")
    void aNullGrantIsNotARestrictionEither() {
        assertFalse(classes.getAcademyClasses(null, null).isEmpty());
    }

    @Test
    @DisplayName("a user holding a grant is offered only what it names")
    void aGrantNarrowsTheList() {
        List<AcademyClass> offered =
                classes.getAcademyClasses(Collections.singletonList(mine), null);

        assertTrue(contains(offered, mine));
        assertFalse(contains(offered, theirs), "the grant is still a narrowing");
    }

    @Test
    @DisplayName("the search term still applies inside a grant")
    void theQueryKeyStillFilters() {
        // Two separate narrowings, and widening the empty-grant case must not
        // have quietly dropped the other one.
        List<AcademyClass> offered =
                classes.getAcademyClasses(Arrays.asList(mine, theirs), theirs.getClassName());

        assertEquals(1, offered.size());
        assertEquals(theirs.getClassName(), offered.get(0).getClassName());
    }

    // ---- what a listing may show --------------------------------------------

    @Test
    @DisplayName("a director sees the whole school")
    void unrestrictedUserIsNotNarrowed() throws Exception {
        String header = tokenFor(user("dir-", Collections.emptyList()));

        assertFalse(guard.isRestricted(header));
        assertTrue(guard.visibleClassGroupIds(header).isEmpty(),
                "empty means unrestricted to every caller that filters with it");
    }

    @Test
    @DisplayName("a granted class resolves to the class group of the same name")
    void aGrantResolvesByName() throws Exception {
        String header = tokenFor(user("teach-", Collections.singletonList(mine)));

        assertTrue(guard.isRestricted(header));
        assertEquals(Collections.singleton(data.classGroup.getId()),
                guard.visibleClassGroupIds(header));
    }

    @Test
    @DisplayName("a grant that no longer resolves shows nothing, not everything")
    void aStaleGrantShowsNothing() throws Exception {
        // The class was renamed, or the year rolled over and the old name is
        // gone. The grant still exists, so the narrowing still applies - it just
        // narrows to nothing. Reading it as "unrestricted" here would hand a
        // teacher the entire school on the day a class is renamed.
        String header = tokenFor(user("stale-", Collections.singletonList(theirs)));

        assertTrue(guard.isRestricted(header));
        Set<Long> visible = guard.visibleClassGroupIds(header);
        assertEquals(1, visible.size(), "a sentinel, so the listing filters to nothing");
        assertFalse(visible.contains(data.classGroup.getId()));
        assertTrue(visible.iterator().next() < 0, "no class group has a negative id");
    }

    @Test
    @DisplayName("acting on a class outside the grant is refused")
    void checkRefusesAClassOutsideTheGrant() throws Exception {
        String header = tokenFor(user("teach2-", Collections.singletonList(mine)));

        // The listing narrows; this is the other half, and it is the half that
        // matters when somebody posts an id the picker never offered them.
        guard.check(header, data.classGroup.getId());
        org.junit.jupiter.api.Assertions.assertThrows(mthiebi.sgs.SGSException.class,
                () -> guard.check(header, -99L));
    }

    // ---- fixture ------------------------------------------------------------

    private AcademyClass academyClass(String name) {
        AcademyClass ac = new AcademyClass();
        ac.setClassName(name);
        ac.setClassLevel(9L);
        em.persist(ac);
        return ac;
    }

    private SystemUser user(String prefix, List<AcademyClass> grant) {
        SystemUser u = new SystemUser();
        u.setUsername(prefix + UUID.randomUUID().toString().substring(0, 8));
        u.setPassword("x");
        u.setActive(true);
        u.setAcademyClassList(new ArrayList<>(grant));
        em.persist(u);
        em.flush();
        return u;
    }

    private String tokenFor(SystemUser user) {
        return "Bearer " + jwt.generateToken("", user.getUsername());
    }

    private boolean contains(List<AcademyClass> list, AcademyClass wanted) {
        return list.stream().anyMatch(c -> c.getId().equals(wanted.getId()));
    }
}
