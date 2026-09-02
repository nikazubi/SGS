package mthiebi.sgs.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.model.AbsenceNotice;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.service.GradeExplainService;
import mthiebi.sgs.gradebook.service.GradeWriteResult;
import mthiebi.sgs.gradebook.service.GradeWriteService;
import mthiebi.sgs.gradebook.service.PeriodTreeLoader;
import mthiebi.sgs.gradebook.service.SpecialValueRegistry;
import mthiebi.sgs.gradebook.service.TemplateGraphLoader;
import mthiebi.sgs.gradebook.service.TemplateVersionResolver;
import mthiebi.sgs.gradebook.service.absence.AbsenceGrid;
import mthiebi.sgs.gradebook.service.absence.AbsenceGridService;
import mthiebi.sgs.gradebook.service.absence.AbsenceNotifier;
import mthiebi.sgs.gradebook.service.absence.AbsenceSettings;
import mthiebi.sgs.gradebook.service.absence.AbsenceSettingsService;
import mthiebi.sgs.gradebook.service.absence.DailyAbsenceGrid;
import mthiebi.sgs.gradebook.service.absence.DailyAbsenceService;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two absence registers, which are two different mechanisms.
 * <p>
 * Daily is rows in its own table: a row means absent, no row means present, and
 * there is no third state to interpret. Monthly is still a journal, and is what
 * keeps the DESCENDANTS reach under test.
 * <p>
 * Also here: the notification window, which is what makes marking a cell safe to
 * get wrong for a quarter of an hour.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AbsenceGridService.class, DailyAbsenceService.class,
        mthiebi.sgs.gradebook.service.absence.DailyAbsenceWriter.class,
        AbsenceNotifier.class,
        mthiebi.sgs.gradebook.service.publish.PublicationService.class,
        mthiebi.sgs.gradebook.service.publish.ChangeRequestService.class,
        mthiebi.sgs.gradebook.service.publish.GuardianNotifier.class,
        mthiebi.sgs.gradebook.service.absence.AbsenceNoticeSender.class,
        AbsenceSettingsService.class,
        GradeWriteService.class, GradeExplainService.class, TemplateGraphLoader.class,
        PeriodTreeLoader.class, TemplateVersionResolver.class, SpecialValueRegistry.class,
        QueryFactoryProvider.class,
        mthiebi.sgs.gradebook.service.conversion.GradeConversionService.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none",
        // The window is what the test drives, so it must not be the production one.
        "sgs.absence.notify-after-minutes=15"
})
class AbsenceGridServiceIT {

    @PersistenceContext
    private EntityManager em;

    /**
     * Mocked rather than wired: a @DataJpaTest slice has no mail sender, and a
     * test that actually sent one would be worse than useless. It also lets the
     * assertions check that a message was really produced, rather than only that
     * the notice was marked resolved - which happens either way.
     */
    @org.springframework.boot.test.mock.mockito.MockBean
    private mthiebi.sgs.SMTP.EmailService emailService;

    @Autowired
    private GradeWriteService gradeWriteService;

    @Autowired
    private mthiebi.sgs.gradebook.service.publish.PublicationService publicationService;

    @Autowired
    private AbsenceGridService absenceGridService;

    @Autowired
    private DailyAbsenceService dailyAbsenceService;

    @Autowired
    private AbsenceNotifier absenceNotifier;

    @Autowired
    private AbsenceSettingsService absenceSettingsService;

    @Autowired
    private mthiebi.sgs.gradebook.service.publish.ChangeRequestService changeRequestService;

    private AbsenceTestData data;

    @BeforeEach
    void setUp() {
        data = new AbsenceTestData(em).build(UUID.randomUUID().toString().substring(0, 8));
        em.flush();
    }

    // ---- the daily register -------------------------------------------------

    @Test
    @DisplayName("a month's school days become a column each, weekends excluded")
    void schoolDaysAreColumns() throws Exception {
        DailyAbsenceGrid grid = dailyAbsenceService.grid(
                data.classGroup.getId(), data.month.getId(), true);

        // September 2025 begins on a Monday: four full weeks of weekdays, then
        // the 29th and 30th. Twenty-two columns rather than thirty.
        assertEquals(22, grid.getColumns().size());
        assertEquals(data.enrollments.size(), grid.getStudents().size());
        assertTrue(grid.getColumns().stream()
                        .noneMatch(c -> c.getDayOfWeek().startsWith("S")),
                "no Saturday or Sunday");
    }

    @Test
    @DisplayName("a mark appears in the grid against its own date")
    void marksLandOnTheRightDay() throws Exception {
        LocalDate day = data.schoolDay(2);
        data.absent(data.enrollments.get(0), day);
        em.flush();
        em.clear();

        DailyAbsenceGrid grid = dailyAbsenceService.grid(
                data.classGroup.getId(), data.month.getId(), true);

        assertEquals(1, grid.getMarks().size());
        assertEquals(day.toString(), grid.getMarks().get(0).getDate());
        assertEquals(data.enrollments.get(0).getId(), grid.getMarks().get(0).getEnrollmentId());
    }

    @Test
    @DisplayName("marking a child who is already absent changes nothing")
    void markingIsIdempotent() throws Exception {
        Enrollment student = data.enrollments.get(0);
        LocalDate day = data.schoolDay(0);

        List<Long> first = mark(day, student, true);
        em.flush();
        List<Long> second = mark(day, student, true);
        em.flush();
        em.clear();

        assertEquals(1, first.size(), "the first mark is new");
        assertTrue(second.isEmpty(), "the second is not, so nobody is told twice");
        assertEquals(1L, absenceRows(student, day),
                "and there is still exactly one row - the constraint holds");
    }

    @Test
    @DisplayName("clearing a mark removes the row rather than blanking it")
    void clearingRemovesTheRow() throws Exception {
        Enrollment student = data.enrollments.get(0);
        LocalDate day = data.schoolDay(0);

        mark(day, student, true);
        em.flush();
        mark(day, student, false);
        em.flush();
        em.clear();

        // The whole reason for the table: "present" is the absence of a row, not
        // a row containing nothing. There is no blank left to interpret.
        assertEquals(0L, absenceRows(student, day));
    }

    @Test
    @DisplayName("a weekend is refused")
    void weekendIsRefused() {
        // Saturday. Nothing generates such a column, so a caller sending one has
        // gone wrong rather than found a legitimate edge.
        assertThrows(SGSException.class,
                () -> mark(LocalDate.of(2025, 9, 6), data.enrollments.get(0), true));
    }

    @Test
    @DisplayName("a date outside the academic year is refused")
    void dateOutsideTheYearIsRefused() {
        // Without the bound a typo lands in a row that is written, indexed, and
        // invisible to every screen that shows a month.
        assertThrows(SGSException.class,
                () -> mark(LocalDate.of(1899, 9, 1), data.enrollments.get(0), true));
    }

    @Test
    @DisplayName("an enrollment from another class is ignored, not marked")
    void foreignEnrollmentIsIgnored() throws Exception {
        AbsenceTestData other = new AbsenceTestData(em)
                .build(UUID.randomUUID().toString().substring(0, 8));
        em.flush();

        Enrollment stranger = other.enrollments.get(0);
        LocalDate day = data.schoolDay(0);

        // The caller supplies enrollment ids, so without this check a
        // coordinator scoped to one class could mark - and notify the parents
        // of - any child in the school.
        List<Long> marked = mark(day, stranger, true);
        em.flush();
        em.clear();

        assertTrue(marked.isEmpty());
        assertEquals(0L, absenceRows(stranger, day));
    }

    @Test
    @DisplayName("days absent over a range is a count, not a stored rollup")
    void daysAbsentCountsARange() {
        Enrollment student = data.enrollments.get(0);
        data.absent(student, data.schoolDay(0));
        data.absent(student, data.schoolDay(1));
        data.absent(student, data.schoolDay(2));
        em.flush();
        em.clear();

        assertEquals(3L, dailyAbsenceService.daysAbsent(
                student.getId(), LocalDate.of(2025, 9, 1), LocalDate.of(2026, 6, 30)));
        assertEquals(2L, dailyAbsenceService.daysAbsent(
                        student.getId(), data.schoolDay(1), data.schoolDay(4)),
                "a narrower range counts fewer");
    }

    // ---- the monthly register ----------------------------------------------

    @Test
    @DisplayName("columns walk every level the journal has a column for")
    void everyLevelBecomesAColumn() throws Exception {
        // The brief's absence table puts reporting periods, trimester totals
        // and the year in one row, so the grid walks the whole subtree rather
        // than one level of it. The fixture has a column on the reporting
        // periods and one on the year, so both appear; a trimester column would
        // appear between them if the journal had one.
        AbsenceGrid grid = absenceGridService.grid(
                data.classGroup.getId(), data.year.getId(), data.monthlyJournal.getUuid(), true);

        assertEquals(data.months.size() + 1, grid.getColumns().size(),
                "the reporting periods, and the year");
        assertEquals("HOURS_MISSED", grid.getComponentCode());
        assertFalse(grid.isToggle(), "hours are typed, not ticked");
    }

    @Test
    @DisplayName("the year comes last and is not typed into")
    void rollupsArePlacedAndReadOnly() throws Exception {
        // Post-order: a trimester's reporting periods, then the trimester, then
        // the year - which is the order the school's table reads left to right.
        AbsenceGrid grid = absenceGridService.grid(
                data.classGroup.getId(), data.year.getId(), data.monthlyJournal.getUuid(), true);

        AbsenceGrid.AbsenceColumn last = grid.getColumns().get(grid.getColumns().size() - 1);
        assertEquals(0, last.getDepth(), "the year is the outermost period");
        assertFalse(last.isEditable(), "a total is computed, not typed");

        AbsenceGrid.AbsenceColumn first = grid.getColumns().get(0);
        assertTrue(first.isEditable(), "the reporting periods are where hours are entered");
        assertEquals("HOURS_MISSED", first.getComponentCode());
    }

    @Test
    @DisplayName("a single reporting period gives a one-column grid")
    void oneReportingPeriodIsOneColumn() throws Exception {
        // This used to be refused: the columns were the chosen period's
        // descendants at one fixed depth, so a month had none and the caller
        // was told so. Now that a column sits on whatever level it names, a
        // month is simply a grid one column wide - which is a narrower answer
        // to a narrower question, not a mistake.
        AbsenceGrid grid = absenceGridService.grid(
                data.classGroup.getId(), data.month.getId(),
                data.monthlyJournal.getUuid(), true);

        assertEquals(1, grid.getColumns().size());
        assertEquals(data.month.getId(), grid.getColumns().get(0).getPeriodId());
    }

    @Test
    @DisplayName("the summary is the report card: a column per trimester, then the year's")
    void summaryPutsTrimestersAndYearInOneRow() throws Exception {
        // The brief's "trimester and final assessment" table. Three of its
        // columns are one component shown at three periods; the rest are the
        // year's own. Nothing in the model implied which columns belong, so the
        // journal says: summary_column.
        data.markSummaryColumns();

        AbsenceGrid grid = absenceGridService.grid(
                data.classGroup.getId(), data.year.getId(), data.monthlyJournal.getUuid(),
                false, null, true);

        // One column per trimester for the marked rollup, then the year's.
        assertTrue(grid.getColumns().size() >= 2, "at least a trimester and a year column");
        assertEquals(0, grid.getColumns().get(grid.getColumns().size() - 1).getDepth(),
                "the year's columns come last");
        assertTrue(grid.getColumns().stream().noneMatch(c -> c.getDepth() == 2),
                "reporting periods are not part of the report card");
    }

    // ---- the two numbers ---------------------------------------------------

    @Test
    @DisplayName("each month carries its own hours and permitted absence")
    void settingsArePerMonth() throws Exception {
        // The bug this pins. Both figures were read against the period the user
        // had chosen - the year - so one entry stood for all nine months, and
        // the permitted figure that turns a parent's chart red had the wrong
        // granularity to do it. The brief has always said per class per month.
        absenceSettingsService.save(data.classGroup.getId(), data.months.get(0).getId(),
                new BigDecimal("120"), new BigDecimal("12"));
        absenceSettingsService.save(data.classGroup.getId(), data.months.get(1).getId(),
                new BigDecimal("100"), new BigDecimal("10"));
        em.flush();
        em.clear();

        AbsenceGrid grid = absenceGridService.grid(
                data.classGroup.getId(), data.year.getId(), data.monthlyJournal.getUuid(), true);

        assertEquals(2, grid.getSettings().size(), "one entry per month, not one for the grid");
        assertEquals(0, settingFor(grid, data.months.get(0)).getTotalAcademicHours()
                .compareTo(new BigDecimal("120")));
        assertEquals(0, settingFor(grid, data.months.get(1)).getTotalAcademicHours()
                .compareTo(new BigDecimal("100")), "October is not September's figure");
        assertEquals(0, settingFor(grid, data.months.get(1)).getPermittedMissedHours()
                .compareTo(new BigDecimal("10")));
    }

    @Test
    @DisplayName("a month nobody has configured simply has no entry")
    void unsetMonthIsAbsentRatherThanZero() throws Exception {
        absenceSettingsService.save(data.classGroup.getId(), data.months.get(0).getId(),
                new BigDecimal("120"), null);
        em.flush();
        em.clear();

        AbsenceGrid grid = absenceGridService.grid(
                data.classGroup.getId(), data.year.getId(), data.monthlyJournal.getUuid(), true);

        assertEquals(1, grid.getSettings().size());
        assertNull(settingFor(grid, data.months.get(0)).getPermittedMissedHours(),
                "half-set is not zero-set");
    }

    @Test
    @DisplayName("a null clears a setting rather than storing a null one")
    void nullClearsSetting() throws Exception {
        absenceSettingsService.save(data.classGroup.getId(), data.month.getId(),
                new BigDecimal("120"), new BigDecimal("12"));
        em.flush();
        absenceSettingsService.save(data.classGroup.getId(), data.month.getId(),
                new BigDecimal("120"), null);
        em.flush();
        em.clear();

        // "Not set" should have one representation, not two.
        Long remaining = em.createQuery(
                        "select count(s) from ClassPeriodSetting s where s.classGroup.id = :c "
                                + "and s.period.id = :p and s.settingKey = :k", Long.class)
                .setParameter("c", data.classGroup.getId())
                .setParameter("p", data.month.getId())
                .setParameter("k", AbsenceSettings.PERMITTED_MISSED_HOURS)
                .getSingleResult();
        assertEquals(0L, remaining);
    }

    @Test
    @DisplayName("a future date is refused, and refused as a future date")
    void futureDateIsRefused() {
        // The grid draws every weekday of the month, the ones still to come
        // included. A mis-click a week ahead used to be accepted, and fifteen
        // minutes later a parent was told about a day that had not happened.
        //
        // The fixture's academic year is fixed at 2025/26, so a date after today
        // is out of that year as well. Asserting only that *something* threw
        // would therefore keep passing with the future-date guard deleted - the
        // year check would cover for it. The message is what distinguishes them.
        LocalDate ahead = LocalDate.now().plusDays(3);
        if (ahead.getDayOfWeek().getValue() > 5) {
            ahead = ahead.plusDays(2);
        }
        final LocalDate target = ahead;

        SGSException refused = assertThrows(SGSException.class,
                () -> mark(target, data.enrollments.get(0), true));
        assertTrue(refused.getMessage() != null
                        && refused.getMessage().contains("მომავალი"),
                "refused as a future date, not merely as out of year: "
                        + refused.getMessage());
    }

    @Test
    @DisplayName("today is accepted when it is a school day inside the year")
    void todayIsAcceptable() throws Exception {
        // The other half of the guard. Without it, "refuses the future" would be
        // satisfied by a guard that refuses everything.
        AbsenceTestData current = new AbsenceTestData(em)
                .aroundToday()
                .build(UUID.randomUUID().toString().substring(0, 8));
        em.flush();

        LocalDate day = LocalDate.now();
        if (day.getDayOfWeek().getValue() > 5) {
            day = day.minusDays(day.getDayOfWeek().getValue() - 5);
        }

        DailyAbsenceService.MarkRequest request = new DailyAbsenceService.MarkRequest();
        request.setEnrollmentId(current.enrollments.get(0).getId());
        request.setAbsent(true);
        List<Long> marked = dailyAbsenceService.mark(
                current.classGroup.getId(), day, Collections.singletonList(request), 1L);

        assertEquals(1, marked.size());
    }

    // ---- the notification window -------------------------------------------

    @Test
    @DisplayName("nothing is sent inside the window")
    void nothingSentInsideTheWindow() {
        Enrollment enrollment = data.enrollments.get(0);
        LocalDate day = data.schoolDay(0);
        absenceNotifier.queue(enrollment, day);
        em.flush();

        absenceNotifier.sendDue();
        em.flush();

        assertNull(notice(enrollment, day).getSentAt(),
                "a mark made a moment ago is still correctable");
    }

    @Test
    @DisplayName("two marks on one day make one notice")
    void oneNoticePerDay() {
        Enrollment enrollment = data.enrollments.get(0);
        LocalDate day = data.schoolDay(0);
        absenceNotifier.queue(enrollment, day);
        absenceNotifier.queue(enrollment, day);
        em.flush();

        Long count = em.createQuery(
                        "select count(n) from AbsenceNotice n where n.enrollment.id = :e", Long.class)
                .setParameter("e", enrollment.getId()).getSingleResult();
        assertEquals(1L, count, "a child absent twice in a day is told about once");
    }

    @Test
    @DisplayName("a mark withdrawn inside the window is cancelled, not sent")
    void withdrawnMarkIsCancelled() {
        Enrollment enrollment = data.enrollments.get(0);
        LocalDate day = data.schoolDay(0);

        // Queued but never written, so the row the job re-reads does not exist -
        // which is exactly what a withdrawn mark leaves behind.
        absenceNotifier.queue(enrollment, day);
        em.flush();
        ageNotice(enrollment, day);

        absenceNotifier.sendDue();
        em.flush();
        em.clear();

        AbsenceNotice after = notice(enrollment, day);
        assertNotNull(after.getSentAt(), "it was resolved");
        assertTrue(after.isCancelled(), "and resolved by not telling anyone");

        // The point of the window: nothing left the building.
        org.mockito.Mockito.verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("a mark that still stands is sent once the window passes")
    void standingMarkIsSent() {
        Enrollment enrollment = data.enrollments.get(0);
        LocalDate day = data.schoolDay(0);

        data.absent(enrollment, day);
        absenceNotifier.queue(enrollment, day);
        em.flush();
        ageNotice(enrollment, day);

        absenceNotifier.sendDue();
        em.flush();
        em.clear();

        AbsenceNotice after = notice(enrollment, day);
        assertNotNull(after.getSentAt());
        assertFalse(after.isCancelled(), "the child really was absent");

        // sendOrThrow, not sendSimpleMail: the latter reports failure in a
        // returned string nobody reads.
        org.mockito.Mockito.verify(emailService)
                .sendOrThrow(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("a cancelled notice does not block a real absence later that day")
    void cancellationDoesNotBlockTheNextNotice() {
        // The failure this exists to prevent: mis-click at 09:00, corrected at
        // 09:05, job cancels it. The child then genuinely goes absent, is marked
        // - and under the old (enrollment, date) unique row plus a status-blind
        // lookup, the parent was never told, silently.
        Enrollment enrollment = data.enrollments.get(0);
        LocalDate day = data.schoolDay(0);

        absenceNotifier.queue(enrollment, day);
        em.flush();
        ageNotice(enrollment, day);
        absenceNotifier.sendDue();
        em.flush();
        em.clear();

        assertTrue(noticesFor(enrollment, day).get(0).isCancelled(), "the mis-click was cancelled");

        // Now the real absence.
        Enrollment reloaded = em.find(Enrollment.class, enrollment.getId());
        data.absent(reloaded, day);
        em.flush();
        absenceNotifier.queue(reloaded, day);
        em.flush();
        em.clear();

        List<AbsenceNotice> all = noticesFor(enrollment, day);
        assertEquals(2, all.size(), "a second notice exists rather than reusing the dead one");
        assertTrue(all.stream().anyMatch(n -> n.getSentAt() == null),
                "and one of them is pending, so the parent will be told");
    }

    @Test
    @DisplayName("a mail failure leaves the notice pending rather than marking it sent")
    void mailFailureIsRetried() {
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("smtp down"))
                .when(emailService).sendOrThrow(org.mockito.ArgumentMatchers.any());

        Enrollment enrollment = data.enrollments.get(0);
        LocalDate day = data.schoolDay(0);
        data.absent(enrollment, day);
        absenceNotifier.queue(enrollment, day);
        em.flush();
        ageNotice(enrollment, day);

        absenceNotifier.sendDue();
        em.flush();
        em.clear();

        AbsenceNotice after = noticesFor(enrollment, day).get(0);
        assertNull(after.getSentAt(), "still owed, so the next run retries it");
        assertFalse(after.isCancelled());
    }

    @Test
    @DisplayName("a student with no guardian email is cancelled, not recorded as told")
    void noAddressIsCancelledNotSent() {
        Enrollment enrollment = data.enrollments.get(0);
        enrollment.getStudent().setGuardianEmail(null);
        LocalDate day = data.schoolDay(0);
        data.absent(enrollment, day);
        absenceNotifier.queue(enrollment, day);
        em.flush();
        ageNotice(enrollment, day);

        absenceNotifier.sendDue();
        em.flush();
        em.clear();

        // "Nobody was told" and "somebody was told" must not look the same
        // afterwards, or an unreachable family is invisible.
        assertTrue(noticesFor(enrollment, day).get(0).isCancelled());
        org.mockito.Mockito.verify(emailService, org.mockito.Mockito.never())
                .sendOrThrow(org.mockito.ArgumentMatchers.any());
    }

    // ---- the yearly total across months ------------------------------------

    @Test
    @DisplayName("hours typed in October do not erase September from the yearly total")
    void yearlyTotalSurvivesAWriteInAnotherMonth() throws Exception {
        // The corruption this exists to prevent. The working set used to load a
        // neighbourhood - the written period, its ancestors, and their children -
        // so an October write did not load September. Evaluating a DESCENDANTS
        // sum against that computed the year's total from October alone and
        // persisted it, wiping September silently.
        Enrollment student = data.enrollments.get(0);

        typeHours(student, data.months.get(0), 6);
        assertEquals(6, yearlyHours(student), "September alone");

        typeHours(student, data.months.get(1), 4);

        assertEquals(10, yearlyHours(student),
                "September's six plus October's four - not October's four alone");
    }

    // ---- publishing releases without freezing ------------------------------

    @Test
    @DisplayName("a published month of hours can still be topped up")
    void publishedHoursCanBeAmended() throws Exception {
        Enrollment student = data.enrollments.get(0);
        typeHours(student, data.months.get(0), 6);

        publicationService.publish(data.classGroup.getId(), data.year.getId(),
                null, data.monthlyJournal.getUuid(), 1L);
        em.flush();
        em.clear();

        // Missed hours accumulate through the month and the coordinator
        // republishes as they do. If publication also froze the cell, that
        // normal path would need the director's signature every time.
        GradeWriteResult result = typeHours(student, data.months.get(0), 8);

        assertTrue(result.getConflicts().isEmpty(),
                "a register does not lock on publish - see GradingTemplate.locksOnPublish");
        assertEquals(8, monthlyHours(student, data.months.get(0)));
    }

    @Test
    @DisplayName("a change request cannot be raised on a journal that does not lock")
    void noChangeRequestOnANonLockingJournal() throws Exception {
        Enrollment student = data.enrollments.get(0);
        typeHours(student, data.months.get(0), 6);
        publicationService.publish(data.classGroup.getId(), data.year.getId(),
                null, data.monthlyJournal.getUuid(), 1L);
        em.flush();
        em.clear();

        Long entryId = em.createQuery(
                        "select g.id from GradeEntry g where g.enrollment.id = :e "
                                + "and g.period.id = :p and g.component.code = 'HOURS_MISSED'", Long.class)
                .setParameter("e", student.getId())
                .setParameter("p", data.months.get(0).getId())
                .getSingleResult();

        mthiebi.sgs.gradebook.service.publish.RaiseChangeRequest request =
                new mthiebi.sgs.gradebook.service.publish.RaiseChangeRequest();
        request.setGradeEntryId(entryId);
        request.setRequestedValue(new BigDecimal("9"));
        request.setReason("ცვლილება");

        // The cell is directly editable after publication, so there is nothing
        // to ask permission for. Left unguarded, a request could sit pending
        // while the coordinator topped the month up, and approving it then
        // wrote the stale value back over the newer one and published that.
        assertThrows(SGSException.class, () -> changeRequestService.raise(request, 1L));
    }

    @Test
    @DisplayName("publishing writes no rows for the months nobody filled in")
    void publishingMaterialisesNothing() throws Exception {
        publicationService.publish(data.classGroup.getId(), data.year.getId(),
                null, data.monthlyJournal.getUuid(), 1L);
        em.flush();
        em.clear();

        // Publishing used to write a row per student per period so that a lock
        // had something to fire on, and the code doing it inserted duplicates
        // and violated uq_grade_cell on the second publish. With no lock there
        // is nothing to assert and nothing to write.
        Long rows = em.createQuery(
                "select count(g) from GradeEntry g where g.templateVersion.id = :v",
                Long.class).setParameter("v", data.monthlyVersion.getId()).getSingleResult();
        assertEquals(0L, rows);
    }

    @Test
    @DisplayName("publishing the same period twice is not an error")
    void publishingTwiceIsSafe() throws Exception {
        typeHours(data.enrollments.get(0), data.months.get(0), 6);

        publicationService.publish(data.classGroup.getId(), data.year.getId(),
                null, data.monthlyJournal.getUuid(), 1L);
        em.flush();
        publicationService.publish(data.classGroup.getId(), data.year.getId(),
                null, data.monthlyJournal.getUuid(), 1L);
        em.flush();

        assertEquals(6, monthlyHours(data.enrollments.get(0), data.months.get(0)));
    }

    // ---- helpers -----------------------------------------------------------

    private AbsenceGrid.PeriodSetting settingFor(AbsenceGrid grid, Period month) {
        return grid.getSettings().stream()
                .filter(x -> x.getPeriodId().equals(month.getId()))
                .findFirst().orElseThrow(() -> new AssertionError("no setting for " + month.getCode()));
    }

    private List<Long> mark(LocalDate date, Enrollment student, boolean absent)
            throws SGSException {
        DailyAbsenceService.MarkRequest request = new DailyAbsenceService.MarkRequest();
        request.setEnrollmentId(student.getId());
        request.setAbsent(absent);
        return dailyAbsenceService.mark(data.classGroup.getId(), date,
                Collections.singletonList(request), 1L);
    }

    private long absenceRows(Enrollment student, LocalDate date) {
        return em.createQuery(
                        "select count(a) from DailyAbsence a "
                                + "where a.enrollment.id = :e and a.absenceDate = :d", Long.class)
                .setParameter("e", student.getId())
                .setParameter("d", date)
                .getSingleResult();
    }

    /**
     * Types a month's missed hours through the real write path, so rollups recompute.
     */
    private GradeWriteResult typeHours(Enrollment student, Period month, int hours)
            throws Exception {
        mthiebi.sgs.gradebook.service.GradeWriteRequest request =
                new mthiebi.sgs.gradebook.service.GradeWriteRequest();
        request.setJournalUuid(data.monthlyJournal.getUuid());
        request.setClassGroupId(data.classGroup.getId());
        request.setSubjectId(null);
        request.setPeriodId(month.getId());

        mthiebi.sgs.gradebook.service.GradeEntryUpdate update =
                new mthiebi.sgs.gradebook.service.GradeEntryUpdate();
        update.setEnrollmentId(student.getId());
        update.setComponentCode("HOURS_MISSED");
        update.setValue(new BigDecimal(hours));
        request.setEntries(Collections.singletonList(update));

        GradeWriteResult result = gradeWriteService.apply(request, 1L);
        em.flush();
        em.clear();
        return result;
    }

    /**
     * The stored yearly sum, read back from the database.
     */
    private int yearlyHours(Enrollment student) {
        return storedValue(student, data.year.getId(), "HOURS_YEAR");
    }

    private int monthlyHours(Enrollment student, Period month) {
        return storedValue(student, month.getId(), "HOURS_MISSED");
    }

    private int storedValue(Enrollment student, Long periodId, String componentCode) {
        List<BigDecimal> values = em.createQuery(
                        "select g.value from GradeEntry g where g.enrollment.id = :e "
                                + "and g.period.id = :p and g.component.code = :c", BigDecimal.class)
                .setParameter("e", student.getId())
                .setParameter("p", periodId)
                .setParameter("c", componentCode)
                .getResultList();
        return values.isEmpty() || values.get(0) == null ? 0 : values.get(0).intValue();
    }

    /**
     * Pushes the notice back past the window, so the job considers it due.
     */
    private void ageNotice(Enrollment enrollment, LocalDate date) {
        em.createQuery("update AbsenceNotice n set n.queuedAt = :t "
                        + "where n.enrollment.id = :e and n.absenceDate = :d")
                .setParameter("t", Instant.now().minus(1, ChronoUnit.HOURS))
                .setParameter("e", enrollment.getId())
                .setParameter("d", date)
                .executeUpdate();
        em.flush();
        em.clear();
    }

    private AbsenceNotice notice(Enrollment enrollment, LocalDate date) {
        return noticesFor(enrollment, date).get(0);
    }

    /**
     * A list, not one row: a resolved notice no longer blocks a later one.
     */
    private List<AbsenceNotice> noticesFor(Enrollment enrollment, LocalDate date) {
        return em.createQuery(
                        "select n from AbsenceNotice n where n.enrollment.id = :e "
                                + "and n.absenceDate = :d order by n.id",
                        AbsenceNotice.class)
                .setParameter("e", enrollment.getId())
                .setParameter("d", date)
                .getResultList();
    }
}
