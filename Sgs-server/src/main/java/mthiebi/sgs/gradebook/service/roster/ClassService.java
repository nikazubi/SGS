package mthiebi.sgs.gradebook.service.roster;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.AcademicYear;
import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.ClassSubject;
import mthiebi.sgs.gradebook.model.PeriodScheme;
import mthiebi.sgs.gradebook.model.School;
import mthiebi.sgs.gradebook.model.Subject;
import mthiebi.sgs.gradebook.model.TeachingAssignment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Classes, and what each of them is taught.
 * <p>
 * The second half is the part that has never had a screen. class_subject was
 * written once by db/006, reordered once by db/010, and adding a subject to a
 * class has been a SQL statement ever since - which also means the teacher
 * names the whole console displays have been unmaintainable since the day they
 * were imported.
 */
@Service
public class ClassService {

    @PersistenceContext
    private EntityManager em;

    // ---- classes ------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<RosterView.ClassRow> list(Long academicYearId) {
        List<ClassGroup> classes = em.createQuery(
                        "select c from ClassGroup c join fetch c.school "
                                + "where (:year is null or c.academicYear.id = :year) "
                                + "order by c.school.ordinal, c.level, c.name", ClassGroup.class)
                .setParameter("year", academicYearId)
                .getResultList();

        return classes.stream().map(c -> new RosterView.ClassRow(
                        c.getId(), c.getName(), c.getLevel(),
                        c.getSchool().getId(), c.getSchool().getName(),
                        c.getAcademicYear().getId(), enrolled(c.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public ClassGroup save(RosterDraft.ClassGroup draft) throws SGSException {
        String name = SubjectService.trimmed(draft.getName());
        if (name.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "კლასის სახელი სავალდებულოა");
        }

        ClassGroup classGroup = draft.getId() == null ? new ClassGroup() : find(draft.getId());

        // The year cannot move under a class that already has children and
        // marks in it: everything hanging off the enrollments would belong to a
        // year the class is no longer in.
        if (draft.getId() != null && draft.getAcademicYearId() != null
                && !draft.getAcademicYearId().equals(classGroup.getAcademicYear().getId())
                && enrolled(draft.getId()) > 0) {
            throw new SGSException(SGSExceptionCode.CONFLICT,
                    "კლასში მოსწავლეებია - სასწავლო წლის შეცვლა შეუძლებელია");
        }

        AcademicYear year = draft.getAcademicYearId() != null
                ? reference(AcademicYear.class, draft.getAcademicYearId(), "სასწავლო წელი")
                : classGroup.getAcademicYear();
        School school = draft.getSchoolId() != null
                ? reference(School.class, draft.getSchoolId(), "სკოლა")
                : classGroup.getSchool();

        requireNameFree(name, school.getId(), year.getId(), draft.getId());

        classGroup.setName(name);
        classGroup.setSchool(school);
        classGroup.setAcademicYear(year);
        if (draft.getLevel() != null) {
            classGroup.setLevel(draft.getLevel());
        }
        classGroup.setPeriodScheme(draft.getPeriodSchemeId() != null
                ? reference(PeriodScheme.class, draft.getPeriodSchemeId(), "პერიოდების სქემა")
                : schemeOf(year));

        em.persist(classGroup);
        em.flush();
        return classGroup;
    }

    /**
     * Only an empty class goes.
     * <p>
     * Anything else would orphan enrollments, and through them every mark,
     * absence and homework target in the class. Emptying it first is a decision
     * somebody has to make child by child, which is the point.
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) throws SGSException {
        ClassGroup classGroup = find(id);
        long students = enrolled(id);
        if (students > 0) {
            throw new SGSException(SGSExceptionCode.CONFLICT,
                    "კლასში " + students + " მოსწავლეა - ჯერ გადაიყვანეთ ისინი");
        }
        em.createQuery("delete from ClassSubject cs where cs.classGroup.id = :id")
                .setParameter("id", id).executeUpdate();
        em.remove(classGroup);
    }

    // ---- what the class is taught -------------------------------------------

    @Transactional(readOnly = true)
    public List<RosterView.ClassSubjectRow> subjects(Long classGroupId) {
        List<ClassSubject> rows = em.createQuery(
                        "select cs from ClassSubject cs join fetch cs.subject "
                                + "where cs.classGroup.id = :id order by cs.sortIndex", ClassSubject.class)
                .setParameter("id", classGroupId)
                .getResultList();

        return rows.stream().map(cs -> new RosterView.ClassSubjectRow(
                        cs.getId(), cs.getSubject().getId(), cs.getSubject().getName(),
                        cs.getSortIndex(), cs.getTeacherName(), teacherUserId(cs.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public ClassSubject addSubject(Long classGroupId, RosterDraft.ClassSubject draft)
            throws SGSException {

        ClassGroup classGroup = find(classGroupId);
        Subject subject = reference(Subject.class, draft.getSubjectId(), "საგანი");

        boolean already = !em.createQuery(
                        "select cs.id from ClassSubject cs "
                                + "where cs.classGroup.id = :c and cs.subject.id = :s", Long.class)
                .setParameter("c", classGroupId).setParameter("s", draft.getSubjectId())
                .getResultList().isEmpty();
        if (already) {
            throw new SGSException(SGSExceptionCode.CONFLICT, "კლასი უკვე სწავლობს ამ საგანს");
        }

        ClassSubject classSubject = new ClassSubject();
        classSubject.setClassGroup(classGroup);
        classSubject.setSubject(subject);
        classSubject.setSortIndex(nextSortIndex(classGroupId));
        classSubject.setTeacherName(SubjectService.blankToNull(draft.getTeacherName()));
        em.persist(classSubject);
        em.flush();

        setTeacherAccount(classSubject, draft.getTeacherUserId());
        return classSubject;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateSubject(Long classSubjectId, RosterDraft.ClassSubject draft)
            throws SGSException {
        ClassSubject classSubject = em.find(ClassSubject.class, classSubjectId);
        if (classSubject == null) {
            throw new SGSException(SGSExceptionCode.NOT_FOUND, "ჩანაწერი ვერ მოიძებნა");
        }
        classSubject.setTeacherName(SubjectService.blankToNull(draft.getTeacherName()));
        em.flush();
        setTeacherAccount(classSubject, draft.getTeacherUserId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeSubject(Long classSubjectId) throws SGSException {
        ClassSubject classSubject = em.find(ClassSubject.class, classSubjectId);
        if (classSubject == null) {
            return;
        }
        // Marks are keyed by subject and period, not by this join row, so they
        // survive - which is right. Removing a subject from a class says it is
        // no longer taught, not that it never was.
        em.createQuery("delete from TeachingAssignment t where t.classSubject.id = :id")
                .setParameter("id", classSubjectId).executeUpdate();
        em.remove(classSubject);
    }

    /**
     * The teaching order, as the ids in the order they should sit.
     */
    @Transactional(rollbackFor = Exception.class)
    public void reorder(Long classGroupId, List<Long> classSubjectIds) throws SGSException {
        if (classSubjectIds == null) {
            return;
        }
        for (int i = 0; i < classSubjectIds.size(); i++) {
            ClassSubject classSubject = em.find(ClassSubject.class, classSubjectIds.get(i));
            // Silently skipped rather than failing the batch: a row deleted in
            // another tab should not lose the ordering of the other twenty.
            if (classSubject != null
                    && classSubject.getClassGroup().getId().equals(classGroupId)) {
                classSubject.setSortIndex(i);
            }
        }
        em.flush();
    }

    // ---- helpers ------------------------------------------------------------

    /**
     * The structured teacher, where there is one.
     * <p>
     * Kept alongside the name rather than replacing it: only 3 of 98 teachers
     * have an account, so a screen that demanded one would be unusable and a
     * migration that dropped the names would lose 95 of them.
     */
    private void setTeacherAccount(ClassSubject classSubject, Long systemUserId) {
        em.createQuery("delete from TeachingAssignment t where t.classSubject.id = :id")
                .setParameter("id", classSubject.getId()).executeUpdate();
        if (systemUserId == null) {
            return;
        }
        TeachingAssignment assignment = new TeachingAssignment();
        assignment.setClassSubject(classSubject);
        assignment.setSystemUserId(systemUserId);
        assignment.setPrimaryTeacher(true);
        em.persist(assignment);
        em.flush();
    }

    private Long teacherUserId(Long classSubjectId) {
        List<Long> ids = em.createQuery(
                        "select t.systemUserId from TeachingAssignment t "
                                + "where t.classSubject.id = :id and t.primaryTeacher = true", Long.class)
                .setParameter("id", classSubjectId)
                .getResultList();
        return ids.isEmpty() ? null : ids.get(0);
    }

    private int nextSortIndex(Long classGroupId) {
        Integer max = em.createQuery(
                "select max(cs.sortIndex) from ClassSubject cs where cs.classGroup.id = :id",
                Integer.class).setParameter("id", classGroupId).getSingleResult();
        return max == null ? 0 : max + 1;
    }

    private long enrolled(Long classGroupId) {
        return em.createQuery(
                        "select count(e) from Enrollment e "
                                + "where e.classGroup.id = :id and e.leftOn is null", Long.class)
                .setParameter("id", classGroupId)
                .getSingleResult();
    }

    private void requireNameFree(String name, Long schoolId, Long yearId, Long selfId)
            throws SGSException {
        List<Long> clash = em.createQuery(
                        "select c.id from ClassGroup c where c.name = :n "
                                + "and c.school.id = :s and c.academicYear.id = :y", Long.class)
                .setParameter("n", name).setParameter("s", schoolId).setParameter("y", yearId)
                .getResultList();
        clash.remove(selfId);
        if (!clash.isEmpty()) {
            throw new SGSException(SGSExceptionCode.CONFLICT,
                    "ამ სკოლაში ასეთი სახელის კლასი უკვე არსებობს");
        }
    }

    private PeriodScheme schemeOf(AcademicYear year) throws SGSException {
        List<PeriodScheme> schemes = em.createQuery(
                        "select s from PeriodScheme s where s.academicYear.id = :y", PeriodScheme.class)
                .setParameter("y", year.getId())
                .getResultList();
        if (schemes.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "სასწავლო წელს პერიოდების სქემა არ აქვს");
        }
        return schemes.get(0);
    }

    private ClassGroup find(Long id) throws SGSException {
        ClassGroup classGroup = em.find(ClassGroup.class, id);
        if (classGroup == null) {
            throw new SGSException(SGSExceptionCode.NOT_FOUND, "კლასი ვერ მოიძებნა");
        }
        return classGroup;
    }

    private <T> T reference(Class<T> type, Long id, String label) throws SGSException {
        T found = id == null ? null : em.find(type, id);
        if (found == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, label + " ვერ მოიძებნა");
        }
        return found;
    }
}
