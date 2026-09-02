package mthiebi.sgs.gradebook;

import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.service.GradeExplainService;
import mthiebi.sgs.gradebook.service.GradeWriteService;
import mthiebi.sgs.gradebook.service.PeriodTreeLoader;
import mthiebi.sgs.gradebook.service.SpecialValueRegistry;
import mthiebi.sgs.gradebook.service.TemplateGraphLoader;
import mthiebi.sgs.gradebook.service.TemplateVersionResolver;
import mthiebi.sgs.gradebook.service.grid.ClassGroupOption;
import mthiebi.sgs.gradebook.service.grid.GradeGrid;
import mthiebi.sgs.gradebook.service.grid.GradeGridService;
import mthiebi.sgs.gradebook.service.grid.GradebookLookupService;
import mthiebi.sgs.gradebook.service.grid.PeriodOption;
import mthiebi.sgs.gradebook.service.grid.SubjectOption;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Draws a real grid from the migrated school data.
 * <p>
 * The other integration tests build their own fixture, which proves the code
 * works on data shaped the way the code expects. This one proves it works on
 * the data the school actually has - 47 classes whose grade had to be parsed
 * out of a Georgian class name, and subjects folded from 143 rows down to 51.
 * <p>
 * Skips rather than fails when the migration has not been run, so a database
 * that only has the schema is not treated as broken.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GradeGridService.class, GradebookLookupService.class, GradeWriteService.class,
        GradeExplainService.class, TemplateGraphLoader.class, PeriodTreeLoader.class,
        TemplateVersionResolver.class, SpecialValueRegistry.class, QueryFactoryProvider.class,
        mthiebi.sgs.gradebook.service.conversion.GradeConversionService.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none"
})
class MigratedDataSmokeIT {

    @Autowired
    private GradebookLookupService lookupService;

    @Autowired
    private GradeGridService gridService;

    @Test
    @DisplayName("a real class, subject and period produce a usable grid")
    void realDataProducesAGrid() throws Exception {

        List<ClassGroupOption> classes = lookupService.classes();
        Assumptions.assumeFalse(classes.isEmpty(),
                "no migrated classes - run db/006_migrate_from_dbo.sql");

        ClassGroupOption classGroup = classes.get(0);
        assertNotNull(classGroup.getSchoolName());
        // The grade is parsed out of the class name (5ა -> 5), so a zero here
        // would mean the parse silently produced nothing.
        assertTrue(classGroup.getLevel() > 0,
                "class " + classGroup.getName() + " has no level");

        List<SubjectOption> subjects = lookupService.subjectsOf(classGroup.getId());
        Assumptions.assumeFalse(subjects.isEmpty(), "no subjects for " + classGroup.getName());

        List<PeriodOption> periods = lookupService.periodsOf(classGroup.getId(), null);
        PeriodOption trimester = periods.stream()
                .filter(p -> "ROLLUP".equals(p.getKind().name()))
                .findFirst().orElse(null);
        Assumptions.assumeTrue(trimester != null, "no trimester periods");

        GradeGrid grid = gridService.load(
                classGroup.getId(), subjects.get(0).getId(), trimester.getId(), null);

        // The seeded template: seven ongoing marks, the average, initial
        // knowledge, progress, final test and the trimester grade.
        assertEquals(12, grid.getColumns().size());
        assertEquals(1, grid.getColumnGroups().size());
        assertFalse(grid.getStudents().isEmpty(),
                "class " + classGroup.getName() + " has no enrolled students");

        // Georgian has to survive the migration as well as the round trip.
        assertTrue(grid.getPeriod().getLabel().contains("ტრიმესტრი"));
        assertFalse(subjects.get(0).getName().contains("?"),
                "subject name was mangled by the collation: " + subjects.get(0).getName());
        assertFalse(grid.getStudents().get(0).getLastName().contains("?"),
                "student name was mangled: " + grid.getStudents().get(0).getLastName());
    }

    @Test
    @DisplayName("the teacher survived the subject fold")
    void teachersSurvivedTheFold() {
        List<ClassGroupOption> classes = lookupService.classes();
        Assumptions.assumeFalse(classes.isEmpty(),
                "no migrated classes - run db/006_migrate_from_dbo.sql");

        // Folding 143 legacy subject rows down to 51 is what destroys the
        // teacher association unless it is moved to class_subject first, and it
        // is a one-way door - so this is worth asserting rather than assuming.
        long withTeacher = classes.stream()
                .flatMap(c -> lookupService.subjectsOf(c.getId()).stream())
                .filter(s -> s.getTeacherName() != null && !s.getTeacherName().isEmpty())
                .count();

        Assumptions.assumeTrue(withTeacher > 0,
                "no teacher names - run db/008_class_subject_teacher.sql");

        SubjectOption sample = classes.stream()
                .flatMap(c -> lookupService.subjectsOf(c.getId()).stream())
                .filter(s -> s.getTeacherName() != null)
                .findFirst().orElseThrow(AssertionError::new);

        assertFalse(sample.getTeacherName().contains("?"),
                "teacher name was mangled: " + sample.getTeacherName());
        // The stored form was "პედაგოგი: <name>"; the label is not the name.
        assertFalse(sample.getTeacherName().contains("პედაგოგი:"),
                "the label was kept as part of the name: " + sample.getTeacherName());
    }
}
