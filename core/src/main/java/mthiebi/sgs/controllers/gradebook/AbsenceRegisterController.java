package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.service.GradeEntryUpdate;
import mthiebi.sgs.gradebook.service.GradeWriteRequest;
import mthiebi.sgs.gradebook.service.GradeWriteResult;
import mthiebi.sgs.gradebook.service.GradeWriteService;
import mthiebi.sgs.gradebook.service.absence.AbsenceGrid;
import mthiebi.sgs.gradebook.service.absence.AbsenceGridService;
import mthiebi.sgs.gradebook.service.absence.AbsenceSettingsService;
import mthiebi.sgs.gradebook.service.absence.DailyAbsenceGrid;
import mthiebi.sgs.gradebook.service.absence.DailyAbsenceService;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * The two absence registers, which are no longer the same kind of thing.
 *
 * <b>Daily</b> is its own table. A row means the child was absent that day;
 * there is no value, no period, no publication and no approval. What reaches a
 * parent is an email the same day. It is a staff working document.
 *
 * <b>Monthly</b> is still a journal: academic hours missed, a real number typed
 * by the coordinator, summed to a yearly total and published to parents. It goes
 * through {@link GradeWriteService} like any other grid.
 * <p>
 * They were one mechanism until phase 10 was revised, and forcing a boolean
 * through a model built for graded marks is what made the register hard. The
 * school had already confirmed the two are independent - one counts days and one
 * counts hours - so this splits along a seam the domain already had.
 * <p>
 * Named AbsenceRegisterController, not AbsenceController: the legacy
 * mthiebi.sgs.controllers.AbsenceController still exists until dbo is dropped at
 * cutover, and two @RestController classes with the same simple name take the
 * same bean name - which does not clash quietly, it stops the whole application
 * context from starting. Caught by ApplicationWiringIT.
 */
@RestController
@RequestMapping("/api/gradebook/absence")
public class AbsenceRegisterController {

    @Autowired
    private DailyAbsenceService dailyAbsenceService;

    @Autowired
    private AbsenceGridService absenceGridService;

    @Autowired
    private GradeWriteService gradeWriteService;

    @Autowired
    private AbsenceSettingsService absenceSettingsService;

    @Autowired
    private ClassScopeGuard classScope;

    @Autowired
    private ActorResolver actorResolver;

    // ---- daily register ----------------------------------------------------

    /**
     * A month of the daily register.
     * <p>
     * The month is still named by a period id - that is how a month is chosen
     * everywhere else - but nothing beneath it is. The columns are the weekdays
     * between the month's own two dates, computed rather than stored.
     */
    @GetMapping("/daily/grid")
    @Secured({AuthConstants.ADD_GRADES, AuthConstants.MANAGE_GRADES})
    public DailyAbsenceGrid dailyGrid(@RequestParam Long classGroupId,
                                      @RequestParam Long monthPeriodId,
                                      @RequestHeader("authorization") String authHeader)
            throws SGSException {
        classScope.check(authHeader, classGroupId);
        // Reported rather than assumed, as the ordinary grid does: a
        // MANAGE_GRADES holder without ADD_GRADES would otherwise be handed an
        // editable register whose every save returns 403.
        return dailyAbsenceService.grid(classGroupId, monthPeriodId,
                has(AuthConstants.ADD_GRADES));
    }

    /**
     * One day of the register.
     * <p>
     * No conflict can come back. Marking is insert-or-delete on a row keyed by
     * child and date, so two coordinators saving the same column converge rather
     * than one of them losing a version check on a boolean.
     *
     * @return the enrollments newly marked absent - what the console needs to
     * know a notice is now owed for.
     */
    @PostMapping("/daily/mark")
    @Secured({AuthConstants.ADD_GRADES})
    public List<Long> markDaily(@RequestBody DailyMarkRequest request,
                                @RequestHeader("authorization") String authHeader)
            throws SGSException {
        classScope.check(authHeader, request.getClassGroupId());
        return dailyAbsenceService.mark(request.getClassGroupId(), parseDate(request.getDate()),
                request.getMarks(), actorResolver.idOf(authHeader));
    }

    // ---- monthly register --------------------------------------------------

    /**
     * @param parentPeriodId the year, whose months become the columns
     */
    @GetMapping("/grid")
    @Secured({AuthConstants.ADD_GRADES, AuthConstants.MANAGE_GRADES})
    public AbsenceGrid grid(@RequestParam Long classGroupId,
                            @RequestParam Long parentPeriodId,
                            @RequestParam String journalUuid,
                            @RequestHeader("authorization") String authHeader)
            throws SGSException {
        classScope.check(authHeader, classGroupId);
        return absenceGridService.grid(classGroupId, parentPeriodId, journalUuid,
                has(AuthConstants.ADD_GRADES));
    }

    /**
     * The report card: the brief's "trimester and final assessment" table.
     * <p>
     * The same transposed grid, showing every column marked for the summary at
     * whatever level it lives on - so the trimester assessment appears once per
     * trimester and the year's columns once each. Read-only: every column in it
     * is calculated, and the marks they are calculated from are entered on the
     * journal's own screen.
     */
    @GetMapping("/summary")
    @Secured({AuthConstants.ADD_GRADES, AuthConstants.MANAGE_GRADES})
    public AbsenceGrid summary(@RequestParam Long classGroupId,
                               @RequestParam Long parentPeriodId,
                               @RequestParam String journalUuid,
                               @RequestParam(required = false) Long subjectId,
                               @RequestHeader("authorization") String authHeader)
            throws SGSException {
        classScope.check(authHeader, classGroupId);
        return absenceGridService.grid(classGroupId, parentPeriodId, journalUuid,
                false, subjectId, true);
    }

    /**
     * One month of hours.
     * <p>
     * The ordinary batch write and nothing else. Queuing a parent notice used to
     * happen here, gated on the period being a single dated day so that it never
     * fired for a month; daily absence now has its own path and took the
     * notification with it.
     */
    @PostMapping("/mark")
    @Secured({AuthConstants.ADD_GRADES})
    public GradeWriteResult mark(@RequestBody AbsenceWrite request,
                                 @RequestHeader("authorization") String authHeader)
            throws SGSException {

        classScope.check(authHeader, request.getClassGroupId());

        GradeWriteRequest write = new GradeWriteRequest();
        write.setJournalUuid(request.getJournalUuid());
        write.setClassGroupId(request.getClassGroupId());
        write.setSubjectId(null);
        write.setPeriodId(request.getPeriodId());
        write.setEntries(request.getEntries());

        return gradeWriteService.apply(write, actorResolver.idOf(authHeader));
    }

    // ---- the month's two numbers -------------------------------------------

    @PostMapping("/settings")
    @Secured({AuthConstants.MANAGE_TOTAL_ABSENCE, AuthConstants.MANAGE_GRADES})
    public void saveSettings(@RequestParam Long classGroupId,
                             @RequestParam Long periodId,
                             @RequestParam(required = false) BigDecimal totalAcademicHours,
                             @RequestParam(required = false) BigDecimal permittedMissedHours,
                             @RequestHeader("authorization") String authHeader)
            throws SGSException {
        classScope.check(authHeader, classGroupId);
        absenceSettingsService.save(classGroupId, periodId,
                totalAcademicHours, permittedMissedHours);
    }

    // ---- helpers -----------------------------------------------------------

    private LocalDate parseDate(String date) throws SGSException {
        if (date == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "თარიღი მითითებული არ არის");
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "თარიღის ფორმატი არასწორია");
        }
    }

    private boolean has(String permission) throws SGSException {
        try {
            return org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(permission));
        } catch (Exception e) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "უფლებები ვერ განისაზღვრა");
        }
    }

    /**
     * One day of the daily register.
     */
    public static class DailyMarkRequest {

        private Long classGroupId;
        /**
         * ISO date. A string so a malformed one is a 400 rather than a 500.
         */
        private String date;
        private List<DailyAbsenceService.MarkRequest> marks;

        public Long getClassGroupId() {
            return classGroupId;
        }

        public void setClassGroupId(Long classGroupId) {
            this.classGroupId = classGroupId;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public List<DailyAbsenceService.MarkRequest> getMarks() {
            return marks;
        }

        public void setMarks(List<DailyAbsenceService.MarkRequest> marks) {
            this.marks = marks;
        }
    }

    /**
     * One column of the monthly register: a period, and the cells under it.
     */
    public static class AbsenceWrite {

        private String journalUuid;
        private Long classGroupId;
        private Long periodId;
        private List<GradeEntryUpdate> entries;

        public String getJournalUuid() {
            return journalUuid;
        }

        public void setJournalUuid(String journalUuid) {
            this.journalUuid = journalUuid;
        }

        public Long getClassGroupId() {
            return classGroupId;
        }

        public void setClassGroupId(Long classGroupId) {
            this.classGroupId = classGroupId;
        }

        public Long getPeriodId() {
            return periodId;
        }

        public void setPeriodId(Long periodId) {
            this.periodId = periodId;
        }

        public List<GradeEntryUpdate> getEntries() {
            return entries;
        }

        public void setEntries(List<GradeEntryUpdate> entries) {
            this.entries = entries;
        }
    }
}
