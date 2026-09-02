package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.gradebook.service.GradeExplainService;
import mthiebi.sgs.gradebook.service.GradeExplanation;
import mthiebi.sgs.gradebook.service.GradeWriteRequest;
import mthiebi.sgs.gradebook.service.GradeWriteResult;
import mthiebi.sgs.gradebook.service.GradeWriteService;
import mthiebi.sgs.gradebook.service.grid.GradeGrid;
import mthiebi.sgs.gradebook.service.grid.GradeGridService;
import mthiebi.sgs.gradebook.service.grid.GradebookLookupService;
import mthiebi.sgs.gradebook.service.grid.ClassGroupOption;
import mthiebi.sgs.gradebook.service.grid.PeriodOption;
import mthiebi.sgs.gradebook.service.grid.SubjectOption;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The gradebook write endpoint.
 * <p>
 * One request per flush of a grid, carrying every dirty cell. The response
 * brings back the recomputed values so the client can patch its cache - the old
 * console posted a request per cell and then invalidated the whole grid, which
 * is most of the latency teachers feel today.
 */
@RestController
@RequestMapping("/api/gradebook")
public class GradebookController {

    @Autowired
    private GradeWriteService gradeWriteService;

    @Autowired
    private GradeGridService gradeGridService;

    @Autowired
    private GradebookLookupService lookupService;

    @Autowired
    private GradeExplainService gradeExplainService;

    @Autowired
    private ActorResolver actorResolver;

    @Autowired
    private ClassScopeGuard classScope;

    private boolean has(String permission) {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> permission.equals(a.getAuthority()));
    }

    // ---- toolbar lookups ------------------------------------------------

    @GetMapping("/classes")
    // Every module that owns a menu entry needs this picker. MANAGE_HOMEWORK was
    // added when homework landed and the four that followed were not, so their
    // dropdowns 403'd and rendered empty - FormikAutocomplete has no catch, so
    // the failure was completely silent.
    @Secured({AuthConstants.ADD_GRADES, AuthConstants.MANAGE_GRADES,
            AuthConstants.MANAGE_HOMEWORK, AuthConstants.MANAGE_SCHEDULE,
            AuthConstants.MANAGE_MENU, AuthConstants.MANAGE_CHARACTERIZATION,
            AuthConstants.MANAGE_TEMPLATES})
    public List<ClassGroupOption> classes(@RequestHeader("authorization") String authHeader)
            throws SGSException {
        // A permission says what someone may do, never where. Without this the
        // picker offers every class in the school to every teacher.
        //
        // visibleClassGroupIds, not allowedClassGroupIds: the latter is empty
        // both for a director and for a restricted user whose grant resolves to
        // nothing, and the filter below cannot tell those apart.
        java.util.Set<Long> allowed = classScope.visibleClassGroupIds(authHeader);
        return lookupService.classes().stream()
                .filter(c -> allowed.isEmpty() || allowed.contains(c.getId()))
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/classes/{classGroupId}/subjects")
    // Every module that owns a menu entry needs this picker. MANAGE_HOMEWORK was
    // added when homework landed and the four that followed were not, so their
    // dropdowns 403'd and rendered empty - FormikAutocomplete has no catch, so
    // the failure was completely silent.
    @Secured({AuthConstants.ADD_GRADES, AuthConstants.MANAGE_GRADES,
            AuthConstants.MANAGE_HOMEWORK, AuthConstants.MANAGE_SCHEDULE,
            AuthConstants.MANAGE_MENU, AuthConstants.MANAGE_CHARACTERIZATION,
            AuthConstants.MANAGE_TEMPLATES})
    public List<SubjectOption> subjects(@RequestHeader("authorization") String authHeader,
                                        @PathVariable Long classGroupId) throws SGSException {
        classScope.check(authHeader, classGroupId);
        return lookupService.subjectsOf(classGroupId);
    }

    /**
     * The class list, for pickers that target particular children.
     * <p>
     * MANAGE_HOMEWORK is on the list because the homework editor needs it and
     * setting homework should not require the ability to enter grades.
     */
    @GetMapping("/classes/{classGroupId}/students")
    // Every module that owns a menu entry needs this picker. MANAGE_HOMEWORK was
    // added when homework landed and the four that followed were not, so their
    // dropdowns 403'd and rendered empty - FormikAutocomplete has no catch, so
    // the failure was completely silent.
    @Secured({AuthConstants.ADD_GRADES, AuthConstants.MANAGE_GRADES,
            AuthConstants.MANAGE_HOMEWORK, AuthConstants.MANAGE_SCHEDULE,
            AuthConstants.MANAGE_MENU, AuthConstants.MANAGE_CHARACTERIZATION,
            AuthConstants.MANAGE_TEMPLATES})
    public List<mthiebi.sgs.gradebook.service.grid.StudentOption> students(
            @RequestHeader("authorization") String authHeader,
            @PathVariable Long classGroupId) throws SGSException {
        classScope.check(authHeader, classGroupId);
        return lookupService.studentsOf(classGroupId);
    }

    /**
     * The absence register's picker: the level above its columns.
     */
    @GetMapping("/classes/{classGroupId}/periods-at")
    @Secured({AuthConstants.ADD_GRADES, AuthConstants.MANAGE_GRADES,
            AuthConstants.MANAGE_TEMPLATES})
    public List<PeriodOption> periodsAtDepth(@RequestHeader("authorization") String authHeader,
                                             @PathVariable Long classGroupId,
                                             @RequestParam int depth) throws SGSException {
        classScope.check(authHeader, classGroupId);
        return lookupService.periodsAtDepth(classGroupId, depth);
    }

    @GetMapping("/classes/{classGroupId}/periods")
    @Secured({AuthConstants.ADD_GRADES, AuthConstants.MANAGE_GRADES,
            AuthConstants.MANAGE_TEMPLATES})
    public List<PeriodOption> periods(@RequestHeader("authorization") String authHeader,
                                      @PathVariable Long classGroupId,
                                      @RequestParam(required = false) String journalUuid)
            throws SGSException {
        classScope.check(authHeader, classGroupId);
        return lookupService.periodsOf(classGroupId, journalUuid);
    }

    /**
     * The whole screen in one call: the columns this template defines, the
     * students, the cells, and what this user may do with them.
     * <p>
     * The page this replaces fetched the grid, the class and the subject
     * separately, drew eleven columns hardcoded in JSX, and refetched
     * everything after each cell edit.
     */
    @GetMapping("/grid")
    @Secured({AuthConstants.ADD_GRADES, AuthConstants.MANAGE_GRADES})
    public GradeGrid grid(@RequestHeader("authorization") String authHeader,
                          @RequestParam Long classGroupId,
                          @RequestParam(required = false) Long subjectId,
                          @RequestParam Long periodId,
                          @RequestParam(required = false) String journalUuid)
            throws SGSException {
        classScope.check(authHeader, classGroupId);
        // Reported rather than assumed: a MANAGE_GRADES holder without
        // ADD_GRADES could otherwise be given an editable grid whose every
        // save returns 403.
        return gradeGridService.load(classGroupId, subjectId, periodId, journalUuid,
                new mthiebi.sgs.gradebook.service.grid.GridCapabilities(
                        has(AuthConstants.ADD_GRADES),
                        has(AuthConstants.ADD_GRADES),
                        has(AuthConstants.MANAGE_GRADES)));
    }

    @PostMapping("/grades/batch")
    @Secured({AuthConstants.ADD_GRADES})
    public GradeWriteResult applyBatch(@RequestHeader("authorization") String authHeader,
                                       @RequestBody GradeWriteRequest request) throws Exception {
        classScope.check(authHeader, request.getClassGroupId());
        return gradeWriteService.apply(request, actorResolver.idOf(authHeader));
    }

    /**
     * Why a calculated cell holds the value it does: which marks fed in, which
     * were skipped and for what reason. Recomputed on request rather than
     * stored, so it can never disagree with the value it explains.
     */
    @GetMapping("/grades/explain")
    @Secured({AuthConstants.MANAGE_GRADES})
    public GradeExplanation explain(@RequestParam Long enrollmentId,
                                    @RequestParam(required = false) Long subjectId,
                                    @RequestParam Long periodId,
                                    @RequestParam String componentCode,
                                    @RequestParam(required = false) String journalUuid,
                                    @RequestHeader("authorization") String authHeader)
            throws SGSException {
        // Addressed by enrollment rather than by class, so the class is
        // resolved from it - without this any MANAGE_GRADES holder could read
        // any student's marks school-wide.
        classScope.checkEnrollment(authHeader, enrollmentId);
        return gradeExplainService.explain(enrollmentId, subjectId, periodId,
                componentCode, journalUuid);
    }

}
