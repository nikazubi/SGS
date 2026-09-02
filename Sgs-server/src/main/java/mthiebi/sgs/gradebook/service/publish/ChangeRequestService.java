package mthiebi.sgs.gradebook.service.publish;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.ChangeRequestStatus;
import mthiebi.sgs.gradebook.model.GradeChangeRequest;
import mthiebi.sgs.gradebook.model.GradeEntry;
import mthiebi.sgs.gradebook.model.Publication;
import mthiebi.sgs.gradebook.repository.GradeChangeRequestRepository;
import mthiebi.sgs.gradebook.repository.GradeEntryRepository;
import mthiebi.sgs.gradebook.repository.PublicationRepository;
import mthiebi.sgs.gradebook.service.CellResult;
import mthiebi.sgs.gradebook.service.GradeEntryUpdate;
import mthiebi.sgs.gradebook.service.GradeWriteRequest;
import mthiebi.sgs.gradebook.service.GradeWriteResult;
import mthiebi.sgs.gradebook.service.GradeWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Changing a grade parents have already seen.
 * <p>
 * A published cell is read-only. Correcting one is a request the teacher
 * explains and the director signs off, which is the flow the school runs on and
 * the reason publication exists at all.
 */
@Service
public class ChangeRequestService {

    @Autowired
    private GradeChangeRequestRepository changeRequestRepository;

    @Autowired
    private GradeEntryRepository gradeEntryRepository;

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    private GradeWriteService gradeWriteService;

    @Autowired
    private GuardianNotifier guardianNotifier;

    @PersistenceContext
    private EntityManager em;

    // ---- raise ----------------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    public ChangeRequestView raise(RaiseChangeRequest request, Long actorUserId)
            throws SGSException {

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "ცვლილების მიზეზის მითითება სავალდებულოა");
        }

        GradeEntry entry = gradeEntryRepository.findById(request.getGradeEntryId())
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "უჯრა ვერ მოიძებნა"));

        // An unpublished cell is simply editable; a request for it would be a
        // queue entry for work the teacher can already do.
        if (!entry.isPublished()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "უჯრა არ არის გამოქვეყნებული — ის უშუალოდ რედაქტირდება");
        }

        // Same reasoning, one step further: a journal that does not freeze on
        // publish leaves its cells editable *after* release too, so there is
        // still nothing to ask permission for. Left unguarded this was worse
        // than pointless - the coordinator could top the month up while a
        // request sat pending, and approving it then wrote the stale requested
        // value back over the newer one and published that.
        if (!entry.getTemplateVersion().getTemplate().isLocksOnPublish()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "ეს ჟურნალი გამოქვეყნების შემდეგაც რედაქტირდება");
        }

        GradeChangeRequest changeRequest = new GradeChangeRequest();
        changeRequest.setGradeEntry(entry);
        changeRequest.setPreviousValue(entry.getPublishedValue());
        changeRequest.setPreviousSpecialValue(entry.getPublishedSpecialValue());
        changeRequest.setRequestedValue(request.getRequestedValue());
        changeRequest.setRequestedSpecialValue(request.getRequestedSpecialValue());
        changeRequest.setReason(request.getReason().trim());
        changeRequest.setStatus(ChangeRequestStatus.PENDING);
        changeRequest.setRequestedBy(actorUserId);
        changeRequest.setRequestedAt(Instant.now());

        try {
            changeRequestRepository.saveAndFlush(changeRequest);
        } catch (DataIntegrityViolationException e) {
            // The filtered unique index caught a second open request. Two
            // teachers submitting at once is exactly when a check-then-insert
            // would have let both through.
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "ამ უჯრაზე უკვე არსებობს განუხილველი მოთხოვნა");
        }
        return ChangeRequestView.of(changeRequest);
    }

    // ---- queue ----------------------------------------------------------

    @Transactional(readOnly = true)
    /**
     * The queue, narrowed to what this caller may see.
     *
     * @param allowedClassGroupIds empty means unrestricted, which is how a
     *                             director is expressed. Otherwise the result is
     *                             filtered to those classes - the endpoint takes
     *                             an optional classGroupId and the console sends
     *                             none, so without this a coordinator scoped to
     *                             one class read every class's requests: student
     *                             names, marks, and the teacher's stated reason.
     */
    public List<ChangeRequestView> queue(ChangeRequestStatus status, Long classGroupId,
                                         java.util.Set<Long> allowedClassGroupIds) {
        return changeRequestRepository.findQueue(status, classGroupId).stream()
                .filter(r -> allowedClassGroupIds.isEmpty()
                        || allowedClassGroupIds.contains(
                        r.getGradeEntry().getEnrollment().getClassGroup().getId()))
                .map(ChangeRequestView::of)
                .collect(Collectors.toList());
    }

    /**
     * The class a request belongs to, so a caller's scope can be checked against it.
     */
    @Transactional(readOnly = true)
    public Long classGroupOf(Long changeRequestId) {
        return changeRequestRepository.findById(changeRequestId)
                .map(r -> r.getGradeEntry().getEnrollment().getClassGroup().getId())
                .orElse(null);
    }

    // ---- decide ---------------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    public ChangeRequestView decide(Long changeRequestId, boolean approve, String comment,
                                    Long actorUserId) throws SGSException {

        GradeChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "მოთხოვნა ვერ მოიძებნა"));

        if (changeRequest.getStatus() != ChangeRequestStatus.PENDING) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "მოთხოვნა უკვე განხილულია");
        }

        // Applied before the status is written, so a refused write leaves the
        // request PENDING even if the rollback were ever to fail. Marking it
        // approved first relied on the transaction to undo the lie.
        if (approve) {
            // The comment is passed in, not read back off the request. It is
            // set below - deliberately, so a refused write leaves the request
            // PENDING - and applyApproved ends by emailing the guardian, which
            // therefore sent every parent the generic fallback body instead of
            // the explanation the director had just typed.
            applyApproved(changeRequest, actorUserId, comment);
        }

        changeRequest.setStatus(approve ? ChangeRequestStatus.APPROVED : ChangeRequestStatus.REJECTED);
        changeRequest.setDecisionComment(comment);
        changeRequest.setDecidedBy(actorUserId);
        changeRequest.setDecidedAt(Instant.now());
        // Rejection changes nothing: the working value stands as it was.

        return ChangeRequestView.of(changeRequest);
    }

    /**
     * Write the value, then release it and everything published that it moved.
     * <p>
     * The second half is the part that matters. Approving a change to an
     * ongoing mark recomputes the average and the trimester grade on the
     * working side as usual - but if only the disputed cell were released,
     * parents would be looking at marks of 7, 8 and 9 beside an average that
     * matches none of them.
     * <p>
     * Downstream cells that were never published are left alone. They are not
     * visible yet, and releasing them here would put them out ahead of their
     * period.
     */
    private void applyApproved(GradeChangeRequest changeRequest, Long actorUserId,
                               String comment)
            throws SGSException {

        GradeEntry entry = changeRequest.getGradeEntry();

        GradeWriteRequest write = new GradeWriteRequest();
        // The cell already names its version, and therefore its journal.
        write.setJournalUuid(entry.getTemplateVersion().getTemplate().getUuid());
        write.setClassGroupId(entry.getEnrollment().getClassGroup().getId());
        write.setSubjectId(entry.getSubject() == null ? null : entry.getSubject().getId());
        write.setPeriodId(entry.getPeriod().getId());

        GradeEntryUpdate update = new GradeEntryUpdate();
        update.setEnrollmentId(entry.getEnrollment().getId());
        update.setComponentCode(entry.getComponent().getCode());
        update.setValue(changeRequest.getRequestedValue());
        update.setSpecialValue(changeRequest.getRequestedSpecialValue());
        // On a calculated column the director's number is an override. Stored
        // as an ordinary derived value it would be recomputed away the next
        // time any of its inputs moved.
        update.setOverride(Boolean.TRUE);
        // No expectedVersion: the director is deciding on the request in front
        // of them, and a competing edit is impossible - the cell is locked.
        write.setEntries(Collections.singletonList(update));

        GradeWriteResult result = gradeWriteService.applyApproved(write, actorUserId);
        if (!result.getConflicts().isEmpty()) {
            // Marking it approved while the write was refused would tell the
            // teacher and the guardian that a change happened when nothing did.
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "ცვლილება ვერ შესრულდა: "
                            + result.getConflicts().get(0).getReason());
        }

        Instant now = Instant.now();
        int released = 0;
        // The disputed cell is released whether or not the write changed
        // anything: the usual case is that a recompute already moved the
        // working value and the request is asking for exactly that to be shown.
        entry.setPublishedValue(entry.getValue());
        entry.setPublishedSpecialValue(entry.getSpecialValue());
        entry.setPublishedAt(now);
        released++;

        released += release(result.getDerived(), entry, now, false);

        Publication publication = new Publication();
        publication.setClassGroup(entry.getEnrollment().getClassGroup());
        publication.setPeriod(entry.getPeriod());
        publication.setSubject(entry.getSubject());
        publication.setPublishedAt(now);
        publication.setPublishedBy(actorUserId);
        publication.setCellCount(released);
        publication.setFromChangeRequest(true);
        publicationRepository.save(publication);

        notifyAfterCommit(entry, comment);
    }

    /**
     * @param force release even if the cell was not previously published - true
     *              only for the disputed cell itself, which by definition was.
     */
    private int release(List<CellResult> cells, GradeEntry disputed, Instant now, boolean force) {
        if (cells.isEmpty()) {
            return 0;
        }
        List<Long> ids = cells.stream()
                .map(c -> keyOf(c, disputed))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return 0;
        }

        int released = 0;
        for (GradeEntry entry : gradeEntryRepository.findAllById(ids)) {
            if (!force && !entry.isPublished()) {
                continue;
            }
            entry.setPublishedValue(entry.getValue());
            entry.setPublishedSpecialValue(entry.getSpecialValue());
            entry.setPublishedAt(now);
            released++;
        }
        return released;
    }

    /**
     * The write result carries cell coordinates rather than row ids, so the
     * entries are looked up by the coordinates they came back with.
     */
    /**
     * The row a recomputed cell refers to.
     * <p>
     * Deliberately not filtered to the disputed cell's journal: the recompute
     * fans across journals, so a cross-journal dependent lives in another one
     * and filtering it out left its published value stale - the very "average
     * that matches none of the marks" the closure release exists to prevent,
     * one journal over. Ambiguity between two journals sharing a code is
     * resolved by preferring a published row, since only those are released.
     */
    private Long keyOf(CellResult cell, GradeEntry disputed) {
        List<GradeEntry> found = em.createQuery(
                        "select g from GradeEntry g "
                                + "where g.enrollment.id = :enrollmentId "
                                + "  and g.period.id = :periodId "
                                + "  and g.component.code = :code "
                                + "  and (:subjectId is null and g.subject is null "
                                + "       or g.subject.id = :subjectId)", GradeEntry.class)
                .setParameter("enrollmentId", cell.getEnrollmentId())
                .setParameter("periodId", cell.getPeriodId())
                .setParameter("code", cell.getComponentCode())
                .setParameter("subjectId", cell.getSubjectId())
                .getResultList();
        if (found.isEmpty()) {
            return null;
        }
        return found.stream()
                .filter(GradeEntry::isPublished)
                .findFirst()
                .orElse(found.get(0))
                .getId();
    }

    private void notifyAfterCommit(GradeEntry entry, String comment) {
        String guardian = entry.getEnrollment().getStudent().getGuardianEmail();
        if (guardian == null || guardian.length() <= 5) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            guardianNotifier.notifyChangeApproved(guardian, comment);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                guardianNotifier.notifyChangeApproved(guardian, comment);
            }
        });
    }
}
