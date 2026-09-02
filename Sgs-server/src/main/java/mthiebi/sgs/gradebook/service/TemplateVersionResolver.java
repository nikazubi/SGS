package mthiebi.sgs.gradebook.service;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.GradingTemplate;
import mthiebi.sgs.gradebook.model.TemplateAssignment;
import mthiebi.sgs.gradebook.model.TemplateVersion;
import mthiebi.sgs.gradebook.repository.GradeEntryRepository;
import mthiebi.sgs.gradebook.repository.GradingTemplateRepository;
import mthiebi.sgs.gradebook.repository.TemplateAssignmentRepository;
import mthiebi.sgs.gradebook.repository.TemplateVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Which version of a journal is in force for a (class, subject, period).
 * <p>
 * Shared deliberately. Reading a grid, writing to it, explaining a cell and
 * exporting must all resolve the same version - if the read path disagreed with
 * the write path the console would render columns the write path then rejects.
 * <p>
 * Takes the journal as a parameter. It used to hardcode TemplateScope.ACADEMIC,
 * so the whole machinery served academic grades only and the ethics and absence
 * journals could never have run on it.
 */
@Service
public class TemplateVersionResolver {

    @Autowired
    private GradeEntryRepository gradeEntryRepository;

    @Autowired
    private TemplateAssignmentRepository templateAssignmentRepository;

    @Autowired
    private TemplateVersionRepository templateVersionRepository;

    @Autowired
    private GradingTemplateRepository gradingTemplateRepository;

    /**
     * A period stays on the version its marks were first entered under.
     * <p>
     * Only when a period is still empty does the currently assigned version
     * apply. Otherwise activating a new template mid-year would silently
     * re-render earlier marks the moment someone corrected one of them - and
     * results that have already gone out to parents must not move underneath
     * them.
     * <p>
     * Bringing an existing period onto a newer version is a deliberate action
     * with a recalculation attached, offered in the journal editor.
     */
    @Transactional(readOnly = true)
    public Resolved resolve(Long classGroupId, Long subjectId, Long periodId,
                            Long templateId) throws SGSException {

        Long journalId = templateId != null ? templateId : defaultJournalId();

        List<Long> inUse = gradeEntryRepository.findTemplateVersionIdsInPeriod(
                classGroupId, periodId, subjectId, journalId);

        if (inUse.size() == 1) {
            TemplateVersion pinned = templateVersionRepository.findById(inUse.get(0))
                    .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                            "შეფასების შაბლონის ვერსია ვერ მოიძებნა"));
            return new Resolved(pinned, true);
        }
        if (inUse.size() > 1) {
            // Two versions in one period means an earlier migration stopped
            // half way. Recomputing against either would corrupt the other.
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "პერიოდში შერეულია შეფასების შაბლონის რამდენიმე ვერსია");
        }

        List<TemplateAssignment> assignments = templateAssignmentRepository.findForClassAndSubject(
                classGroupId, subjectId, journalId);
        if (!assignments.isEmpty()) {
            return new Resolved(assignments.get(0).getTemplateVersion(), false);
        }

        // A class created after the journal was activated has no assignment.
        // Falling back to the journal's active version means a new class - or
        // next year's classes - can open every journal without an administrator
        // re-activating each one by hand.
        List<TemplateVersion> active = templateVersionRepository.findByTemplate(journalId).stream()
                .filter(v -> v.getStatus() == mthiebi.sgs.gradebook.model.TemplateVersionStatus.ACTIVE
                        || v.getStatus() == mthiebi.sgs.gradebook.model.TemplateVersionStatus.LOCKED)
                .collect(java.util.stream.Collectors.toList());
        if (!active.isEmpty()) {
            return new Resolved(active.get(active.size() - 1), false);
        }
        throw new SGSException(SGSExceptionCode.BAD_REQUEST, "ჟურნალს არ აქვს აქტიური ვერსია");
    }

    /**
     * Callers that predate journals get the first one in the menu.
     * <p>
     * Kept so the grid, write and export paths stay callable without a journal
     * while the console is being moved onto them; a request that names a
     * journal always wins.
     */
    @Transactional(readOnly = true)
    public Long defaultJournalId() throws SGSException {
        return gradingTemplateRepository.findActive().stream()
                .findFirst()
                .map(GradingTemplate::getId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "ჟურნალი ვერ მოიძებნა"));
    }

    @Transactional(readOnly = true)
    public GradingTemplate journalByUuid(String uuid) throws SGSException {
        if (uuid == null || uuid.isEmpty()) {
            return gradingTemplateRepository.findById(defaultJournalId())
                    .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                            "ჟურნალი ვერ მოიძებნა"));
        }
        return gradingTemplateRepository.findByUuid(uuid)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "ჟურნალი ვერ მოიძებნა: " + uuid));
    }

    /**
     * The version, and whether the period is already committed to it.
     * <p>
     * {@code pinned} is what the editor needs before offering to move a period
     * onto a newer version: an unpinned period can simply be reassigned, a
     * pinned one has marks that would have to be recalculated.
     */
    public static class Resolved {

        private final TemplateVersion version;
        private final boolean pinned;

        Resolved(TemplateVersion version, boolean pinned) {
            this.version = version;
            this.pinned = pinned;
        }

        public TemplateVersion getVersion() {
            return version;
        }

        public boolean isPinned() {
            return pinned;
        }
    }
}
