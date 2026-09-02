package mthiebi.sgs.gradebook.service.absence;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.DailyAbsence;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.model.Student;
import mthiebi.sgs.gradebook.repository.DailyAbsenceRepository;
import mthiebi.sgs.gradebook.repository.EnrollmentRepository;
import mthiebi.sgs.gradebook.repository.PeriodRepository;
import mthiebi.sgs.gradebook.service.grid.GridStudent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The daily register: mark a child absent, or unmark them.
 * <p>
 * Small on purpose. As a journal this went through the grade write path -
 * template resolution, a graph, a working set spanning the period tree, an
 * evaluator, a recompute pass, an optimistic-lock check and a publication lock -
 * to record one bit. All of that existed for grades and none of it had anything
 * to say about a tick.
 * <p>
 * Marking is idempotent in both directions: marking an absent child leaves them
 * absent, clearing a present one leaves them present. There is no conflict to
 * report because two people setting the same boolean agree by construction.
 */
@Service
public class DailyAbsenceService {

    @Autowired
    private DailyAbsenceRepository dailyAbsenceRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PeriodRepository periodRepository;

    @Autowired
    private AbsenceNotifier absenceNotifier;

    @Autowired
    private DailyAbsenceWriter writer;

    private static final Logger log = LoggerFactory.getLogger(DailyAbsenceService.class);

    // ---- read ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public DailyAbsenceGrid grid(Long classGroupId, Long monthPeriodId, boolean canEdit)
            throws SGSException {

        Period month = periodRepository.findById(monthPeriodId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "პერიოდი ვერ მოიძებნა"));
        if (month.getStartsOn() == null || month.getEndsOn() == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "პერიოდს თარიღები არ აქვს");
        }

        List<Enrollment> enrollments = enrollmentRepository.findActiveByClassGroup(classGroupId);
        DailyAbsenceGrid grid = new DailyAbsenceGrid();
        grid.setMonthPeriodId(month.getId());
        grid.setMonthLabel(month.getLabel());
        grid.setCanEdit(canEdit);

        for (LocalDate day : schoolDays(month.getStartsOn(), month.getEndsOn())) {
            grid.getColumns().add(new DailyAbsenceGrid.DayColumn(
                    day.toString(), day.getDayOfMonth(), day.getDayOfWeek().name()));
        }

        appendStudents(grid, enrollments);

        if (enrollments.isEmpty()) {
            return grid;
        }
        List<Long> enrollmentIds = enrollments.stream()
                .map(Enrollment::getId).collect(Collectors.toList());

        Map<Long, Long> counts = new HashMap<>();
        for (DailyAbsence absence : dailyAbsenceRepository.findInRange(
                enrollmentIds, month.getStartsOn(), month.getEndsOn())) {
            Long id = absence.getEnrollment().getId();
            grid.getMarks().add(new DailyAbsenceGrid.Mark(id, absence.getAbsenceDate().toString()));
            counts.merge(id, 1L, Long::sum);
        }
        for (Long id : enrollmentIds) {
            grid.getTotals().add(new DailyAbsenceGrid.Total(id, counts.getOrDefault(id, 0L)));
        }
        return grid;
    }

    /**
     * Days absent between two dates.
     * <p>
     * The yearly total, and any other total anyone asks for. It was a DERIVED
     * column with a SUM rule over a DESCENDANTS term, recomputed and stored on
     * every single mark; it is a count over an index.
     */
    @Transactional(readOnly = true)
    public long daysAbsent(Long enrollmentId, LocalDate from, LocalDate to) {
        return dailyAbsenceRepository.countInRange(enrollmentId, from, to);
    }

    // ---- write --------------------------------------------------------------

    /**
     * One day of the register, for as many children as the caller names.
     *
     * The class is checked against each enrollment rather than trusted from the
     * request: the caller supplies the ids, so without it a coordinator scoped
     * to one class could mark - and notify the parents of - any child in the
     * school.
     *
     * @return the enrollments newly marked absent, which is what a notice is
     *         owed for. A child already absent is not in it: nothing changed, so
     *         nobody needs telling twice.
     */
    /**
     * Deliberately <b>not</b> transactional. Each cell is written in its own
     * transaction by {@link DailyAbsenceWriter}, so a race lost on one child
     * costs that cell rather than rolling back the whole column - and a
     * constraint violation leaves its own session unusable, not this one's.
     */
    public List<Long> mark(Long classGroupId, LocalDate date, List<MarkRequest> marks,
                           Long actorUserId) throws SGSException {

        requireSchoolDay(date);
        requireNotFuture(date);
        requireWithinYear(classGroupId, date);

        Map<Long, Enrollment> byId = enrollmentRepository.findActiveByClassGroup(classGroupId)
                .stream().collect(Collectors.toMap(Enrollment::getId, e -> e));

        List<Long> newlyAbsent = new ArrayList<>();
        for (MarkRequest mark : marks == null ? Collections.<MarkRequest>emptyList() : marks) {
            Enrollment enrollment = byId.get(mark.getEnrollmentId());
            if (enrollment == null) {
                // Not in this class, or not active in it. Silently skipped
                // rather than failing the batch: the register is saved a column
                // at a time and one stale row should not lose the other thirty.
                continue;
            }
            try {
                if (mark.isAbsent()) {
                    if (writer.markAbsent(enrollment, date, actorUserId)) {
                        newlyAbsent.add(enrollment.getId());
                    }
                } else {
                    writer.clear(enrollment.getId(), date);
                }
            } catch (DataIntegrityViolationException e) {
                // Lost the race to another coordinator marking the same child.
                // The outcome is the one that was wanted - the child is marked -
                // so this is not an error to report; it is simply not *this*
                // request's insert, and the winner's notice is already queued.
                log.debug("daily absence for {} on {} was written concurrently",
                        enrollment.getId(), date);
            }
        }

        // Queued after the rows are written, so a parent is never told about a
        // mark that did not land. Nothing is sent for a quarter of an hour and
        // the row is re-read first, so a mis-click corrected inside the window
        // reaches nobody.
        //
        // A notice that cannot be queued must not cost the mark: the register is
        // the record, the email is a courtesy on top of it.
        for (Long enrollmentId : newlyAbsent) {
            try {
                absenceNotifier.queue(byId.get(enrollmentId), date);
            } catch (RuntimeException e) {
                log.warn("absence notice not queued for {} on {}: {}",
                        enrollmentId, date, e.getMessage());
            }
        }
        return newlyAbsent;
    }

    // ---- validation ---------------------------------------------------------

    /**
     * School days only.
     * <p>
     * Refused rather than accepted quietly: a register with a Saturday in it is
     * a register nobody can reconcile against a timetable, and the columns this
     * service generates never include one, so a weekend date is a caller that
     * has gone wrong rather than a legitimate edge.
     */
    private void requireSchoolDay(LocalDate date) throws SGSException {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "შაბათ-კვირას გაცდენა არ აღირიცხება");
        }
    }

    /**
     * Not in the future.
     * <p>
     * A register records what happened. The grid draws every weekday of the
     * month, including the ones still to come, so a mis-click a week ahead was
     * accepted - and fifteen minutes later a parent was told their child missed
     * a day that has not happened yet.
     */
    private void requireNotFuture(LocalDate date) throws SGSException {
        if (date.isAfter(LocalDate.now())) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "მომავალი თარიღით გაცდენა არ აღირიცხება");
        }
    }

    /**
     * Inside the class's own academic year.
     * <p>
     * Nothing in the shape of a date stops a typo landing in 1899, and without
     * a bound the row would be written, indexed, and invisible to every screen
     * that shows a month.
     */
    private void requireWithinYear(Long classGroupId, LocalDate date) throws SGSException {
        List<Enrollment> enrollments = enrollmentRepository.findActiveByClassGroup(classGroupId);
        if (enrollments.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "კლასში მოსწავლეები ვერ მოიძებნა");
        }
        Long schemeId = enrollments.get(0).getClassGroup().getPeriodScheme().getId();
        boolean inside = periodRepository.findByScheme(schemeId).stream()
                .filter(p -> p.getDepth() == 0)
                .anyMatch(p -> p.getStartsOn() != null && p.getEndsOn() != null
                        && !date.isBefore(p.getStartsOn()) && !date.isAfter(p.getEndsOn()));
        if (!inside) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "თარიღი სასწავლო წლის გარეთაა");
        }
    }

    // ---- helpers ------------------------------------------------------------

    /**
     * Weekdays between two dates, inclusive.
     * <p>
     * Computed rather than stored. This used to be 217 dated period rows,
     * generated by a SQL script whose weekend filter depended on the session's
     * DATEFIRST setting - so a differently-configured login would have produced
     * Saturday columns and dropped every Monday. {@link DayOfWeek} has no such
     * setting.
     */
    static List<LocalDate> schoolDays(LocalDate from, LocalDate to) {
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY
                    && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                days.add(day);
            }
        }
        return days;
    }

    private void appendStudents(DailyAbsenceGrid grid, List<Enrollment> enrollments) {
        List<Enrollment> ordered = new ArrayList<>(enrollments);
        ordered.sort((a, b) -> {
            Student x = a.getStudent();
            Student y = b.getStudent();
            int byLast = nullSafe(x.getLastName()).compareTo(nullSafe(y.getLastName()));
            return byLast != 0 ? byLast
                    : nullSafe(x.getFirstName()).compareTo(nullSafe(y.getFirstName()));
        });
        int index = 1;
        for (Enrollment e : ordered) {
            grid.getStudents().add(new GridStudent(e.getId(), e.getStudent().getId(),
                    e.getStudent().getFirstName(), e.getStudent().getLastName(), index++));
        }
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /**
     * One child on one day: absent, or not.
     */
    public static class MarkRequest {

        private Long enrollmentId;
        private boolean absent;

        public Long getEnrollmentId() {
            return enrollmentId;
        }

        public void setEnrollmentId(Long enrollmentId) {
            this.enrollmentId = enrollmentId;
        }

        public boolean isAbsent() {
            return absent;
        }

        public void setAbsent(boolean absent) {
            this.absent = absent;
        }
    }
}
