package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.gradebook.service.roster.AcademicYearService;
import mthiebi.sgs.gradebook.service.roster.ClassService;
import mthiebi.sgs.gradebook.service.roster.RosterDraft;
import mthiebi.sgs.gradebook.service.roster.RosterView;
import mthiebi.sgs.gradebook.service.roster.StudentService;
import mthiebi.sgs.gradebook.service.roster.EnrollmentService;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Students, classes and subjects, against the model the rewrite actually reads.
 *
 * <h3>Why this is not StudentController</h3>
 * <p>
 * Because {@code mthiebi.sgs.controllers.StudentController} already exists -
 * the legacy one, writing dbo - and two @RestController classes with the same
 * simple name are one bean name, which is a ConflictingBeanDefinitionException
 * and an application that does not start at all. That has happened here once
 * already, from a stale class in an un-cleaned war. One controller for the
 * whole roster avoids three chances to repeat it, and the endpoints read better
 * grouped anyway.
 * <p>
 * The legacy pages stay live until these screens replace them. They write dbo
 * and this writes sgs; nothing here syncs the two, deliberately - see
 * REWRITE-ROSTER.md.
 */
@RestController
@RequestMapping("/api/gradebook/roster")
public class RosterController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private ClassService classService;

    @Autowired
    private mthiebi.sgs.gradebook.service.roster.SubjectService subjectService;

    @Autowired
    private AcademicYearService academicYearService;

    @Autowired
    private EnrollmentService enrollmentService;

    // ---- years and schools ---------------------------------------------------

    @GetMapping("/years")
    @Secured({AuthConstants.VIEW_ACADEMY_CLASS, AuthConstants.MANAGE_ACADEMY_CLASS,
            AuthConstants.VIEW_STUDENT, AuthConstants.MANAGE_STUDENT})
    public List<RosterView.YearRow> years() {
        return academicYearService.list();
    }

    @GetMapping("/schools")
    @Secured({AuthConstants.VIEW_ACADEMY_CLASS, AuthConstants.MANAGE_ACADEMY_CLASS})
    public List<RosterView.SchoolRow> schools() {
        return academicYearService.schools();
    }

    /**
     * Starting the next year: the year, its periods, optionally its classes.
     */
    @PostMapping("/years")
    @Secured({AuthConstants.MANAGE_ACADEMY_CLASS})
    public Long startYear(@RequestBody RosterDraft.NewYear draft) throws SGSException {
        return academicYearService.startYear(draft).getId();
    }

    @PostMapping("/years/{id}/current")
    @Secured({AuthConstants.MANAGE_ACADEMY_CLASS})
    public void makeCurrent(@PathVariable Long id) throws SGSException {
        academicYearService.makeCurrent(id);
    }

    // ---- subjects ------------------------------------------------------------

    @GetMapping("/subjects")
    @Secured({AuthConstants.VIEW_SUBJECT, AuthConstants.MANAGE_SUBJECT})
    public List<RosterView.SubjectRow> subjects(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return subjectService.list(includeInactive);
    }

    @PostMapping("/subjects")
    @Secured({AuthConstants.MANAGE_SUBJECT})
    public Long saveSubject(@RequestBody RosterDraft.Subject draft) throws SGSException {
        return subjectService.save(draft).getId();
    }

    @DeleteMapping("/subjects/{id}")
    @Secured({AuthConstants.MANAGE_SUBJECT})
    public void deleteSubject(@PathVariable Long id) throws SGSException {
        subjectService.delete(id);
    }

    // ---- classes -------------------------------------------------------------

    @GetMapping("/classes")
    @Secured({AuthConstants.VIEW_ACADEMY_CLASS, AuthConstants.MANAGE_ACADEMY_CLASS,
            AuthConstants.VIEW_STUDENT, AuthConstants.MANAGE_STUDENT})
    public List<RosterView.ClassRow> classes(@RequestParam(required = false) Long academicYearId) {
        return classService.list(academicYearId);
    }

    @PostMapping("/classes")
    @Secured({AuthConstants.MANAGE_ACADEMY_CLASS})
    public Long saveClass(@RequestBody RosterDraft.ClassGroup draft) throws SGSException {
        return classService.save(draft).getId();
    }

    @DeleteMapping("/classes/{id}")
    @Secured({AuthConstants.MANAGE_ACADEMY_CLASS})
    public void deleteClass(@PathVariable Long id) throws SGSException {
        classService.delete(id);
    }

    // ---- what a class is taught ----------------------------------------------

    @GetMapping("/classes/{id}/subjects")
    @Secured({AuthConstants.VIEW_ACADEMY_CLASS, AuthConstants.MANAGE_ACADEMY_CLASS})
    public List<RosterView.ClassSubjectRow> classSubjects(@PathVariable Long id) {
        return classService.subjects(id);
    }

    @PostMapping("/classes/{id}/subjects")
    @Secured({AuthConstants.MANAGE_ACADEMY_CLASS})
    public Long addClassSubject(@PathVariable Long id,
                                @RequestBody RosterDraft.ClassSubject draft) throws SGSException {
        return classService.addSubject(id, draft).getId();
    }

    @PostMapping("/classes/subjects/{classSubjectId}")
    @Secured({AuthConstants.MANAGE_ACADEMY_CLASS})
    public void updateClassSubject(@PathVariable Long classSubjectId,
                                   @RequestBody RosterDraft.ClassSubject draft)
            throws SGSException {
        classService.updateSubject(classSubjectId, draft);
    }

    @DeleteMapping("/classes/subjects/{classSubjectId}")
    @Secured({AuthConstants.MANAGE_ACADEMY_CLASS})
    public void removeClassSubject(@PathVariable Long classSubjectId) throws SGSException {
        classService.removeSubject(classSubjectId);
    }

    @PostMapping("/classes/{id}/subjects/reorder")
    @Secured({AuthConstants.MANAGE_ACADEMY_CLASS})
    public void reorderClassSubjects(@PathVariable Long id,
                                     @RequestBody RosterDraft.Reorder draft) throws SGSException {
        classService.reorder(id, draft.getClassSubjectIds());
    }

    // ---- students ------------------------------------------------------------

    @GetMapping("/students")
    @Secured({AuthConstants.VIEW_STUDENT, AuthConstants.MANAGE_STUDENT})
    public List<RosterView.StudentRow> students(
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Long classGroupId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return studentService.list(academicYearId, classGroupId, search, includeInactive);
    }

    @PostMapping("/students")
    @Secured({AuthConstants.MANAGE_STUDENT})
    public Long saveStudent(@RequestBody RosterDraft.Student draft,
                            @RequestParam(required = false) Long academicYearId)
            throws SGSException {
        return studentService.save(draft, academicYearId).getId();
    }

    @PostMapping("/students/{id}/deactivate")
    @Secured({AuthConstants.MANAGE_STUDENT})
    public void deactivateStudent(@PathVariable Long id) throws SGSException {
        studentService.deactivate(id);
    }

    @GetMapping("/students/{id}/history")
    @Secured({AuthConstants.VIEW_STUDENT, AuthConstants.MANAGE_STUDENT})
    public List<RosterView.PlacementRow> history(
            @PathVariable Long id, @RequestParam(required = false) Long academicYearId) {
        return studentService.history(id, academicYearId);
    }

    // ---- enrollment, addressed directly --------------------------------------
    //
    // Moving and leaving are their own actions rather than fields on the student
    // form: both are dated events, and burying a date that rewrites the class
    // register inside a "save" button is how somebody moves a child by accident.

    @PostMapping("/enrollments/{id}/move")
    @Secured({AuthConstants.MANAGE_STUDENT})
    public void move(@PathVariable Long id, @RequestBody RosterDraft.Move draft)
            throws SGSException {
        enrollmentService.move(id, draft.getClassGroupId(), draft.getOn());
    }

    @PostMapping("/enrollments/{id}/leave")
    @Secured({AuthConstants.MANAGE_STUDENT})
    public void leave(@PathVariable Long id, @RequestBody RosterDraft.Move draft)
            throws SGSException {
        enrollmentService.leave(id, draft.getOn());
    }
}
