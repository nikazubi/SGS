package mthiebi.sgs.gradebook.service.grid;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.ClassSubject;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.model.Subject;
import mthiebi.sgs.gradebook.repository.ClassGroupRepository;
import mthiebi.sgs.gradebook.repository.PeriodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * What the grade entry toolbar picks from: classes, the subjects a class takes,
 * and the periods of its scheme.
 * <p>
 * Deliberately thin. These were previously served by the legacy controllers
 * over the {@code dbo} entities, which the new grid cannot use - it works in
 * enrollments and periods, not in students and hardcoded trimester numbers.
 */
@Service
public class GradebookLookupService {

    @Autowired
    private mthiebi.sgs.gradebook.repository.EnrollmentRepository enrollmentRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private PeriodRepository periodRepository;

    @Autowired
    private mthiebi.sgs.gradebook.service.TemplateVersionResolver templateVersionResolver;

    @Transactional(readOnly = true)
    public List<ClassGroupOption> classes() {
        return classGroupRepository.findForCurrentYear().stream()
                .map(c -> new ClassGroupOption(c.getId(), c.getName(), c.getLevel(),
                        c.getSchool().getName(), c.getPeriodScheme().getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubjectOption> subjectsOf(Long classGroupId) {
        return classGroupRepository.findClassSubjectsOf(classGroupId).stream()
                .map(this::toOption)
                .collect(Collectors.toList());
    }

    /**
     * The class's periods at one level of the tree.
     * <p>
     * Both absence registers pick the level *above* their columns - a month for
     * the daily one, the year for the monthly - which the journal-aware lookup
     * cannot give them, because that narrows to the journal's own level. The
     * daily register is not a journal at all, so it has nothing else to ask.
     * <p>
     * A flat filter, not a reach: this answers "what may the user choose?",
     * which is a different question from "what does this period touch?" and is
     * deliberately not PeriodReach's business.
     */
    @Transactional(readOnly = true)
    public List<PeriodOption> periodsAtDepth(Long classGroupId, int depth) {
        ClassGroup classGroup = classGroupRepository.findById(classGroupId).orElse(null);
        if (classGroup == null) {
            return java.util.Collections.emptyList();
        }
        return periodRepository.findByScheme(classGroup.getPeriodScheme().getId()).stream()
                .filter(p -> p.getDepth() == depth)
                .map(p -> new PeriodOption(p.getId(), p.getCode(), p.getLabel(), p.getKind(),
                        p.getDepth(), p.getOrdinal(),
                        p.getParent() == null ? null : p.getParent().getId()))
                .collect(Collectors.toList());
    }

    /**
     * The class list, for anything that targets particular children.
     * <p>
     * Homework aimed at a few students needs it; so will phase 9's
     * characterizations. Ordered as every other list in the system is.
     */
    @Transactional(readOnly = true)
    public List<StudentOption> studentsOf(Long classGroupId) {
        // findActiveByClassGroup already excludes anyone who has left - leftOn
        // is the flag, and a student who has gone should not be offered homework.
        return enrollmentRepository.findActiveByClassGroup(classGroupId).stream()
                .map(e -> new StudentOption(e.getId(), e.getStudent().getId(),
                        e.getStudent().getLastName() + " " + e.getStudent().getFirstName()))
                .collect(Collectors.toList());
    }

    /**
     * The periods a journal is actually filled in on.
     * <p>
     * Resolved from the class rather than taken as a parameter - asking the
     * caller for a scheme id invites a grid request whose period belongs to a
     * different one.
     * <p>
     * Narrowed to the journal's own level, plus the year when it has columns
     * that roll up there. A monthly journal offering trimesters and weeks in
     * its dropdown would just be a way to open an empty grid.
     */
    @Transactional(readOnly = true)
    public List<PeriodOption> periodsOf(Long classGroupId, String journalUuid)
            throws SGSException {
        ClassGroup classGroup = classGroupRepository.findById(classGroupId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "კლასი ვერ მოიძებნა"));

        List<PeriodOption> all = periodRepository
                .findByScheme(classGroup.getPeriodScheme().getId()).stream()
                .map(this::toOption)
                .collect(Collectors.toList());

        if (journalUuid == null || journalUuid.isEmpty()) {
            return all;
        }
        int depth = templateVersionResolver.journalByUuid(journalUuid)
                .getFrequency().getDepth();
        return all.stream()
                .filter(p -> p.getDepth() == depth || p.getDepth() == 0)
                .collect(Collectors.toList());
    }

    private SubjectOption toOption(ClassSubject cs) {
        Subject s = cs.getSubject();
        return new SubjectOption(s.getId(), s.getName(), s.getShortName(), cs.getTeacherName());
    }

    private PeriodOption toOption(Period p) {
        return new PeriodOption(p.getId(), p.getCode(), p.getLabel(), p.getKind(),
                p.getDepth(), p.getOrdinal(),
                p.getParent() == null ? null : p.getParent().getId());
    }
}
