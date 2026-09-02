package mthiebi.sgs.gradebook.service.publish;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.engine.PeriodReach;
import mthiebi.sgs.gradebook.model.GradeEntry;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.model.Publication;
import mthiebi.sgs.gradebook.model.Subject;
import mthiebi.sgs.gradebook.repository.EnrollmentRepository;
import mthiebi.sgs.gradebook.repository.GradeEntryRepository;
import mthiebi.sgs.gradebook.repository.PeriodRepository;
import mthiebi.sgs.gradebook.repository.PublicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Releases grades to parents.
 * <p>
 * Until this runs, the journal is a working document: teachers edit it freely
 * and parents see nothing.
 * <p>
 * What happens afterwards depends on the journal. A grades journal locks: every
 * released cell is read-only and changing one needs the director. A register
 * does not - see GradingTemplate.locksOnPublish. Absence accumulates through the
 * month and is republished as it does, so a lock would put a signature between
 * the coordinator and every top-up.
 * <p>
 * The legacy equivalent wrote a timestamp and let parent queries filter on
 * `grade.createTime < that`. It did not hold - grades are updated in place, so
 * createTime never moved and post-publication edits reached parents at once.
 * Publication is per cell for that reason.
 */
@Service
public class PublicationService {

    @Autowired
    private mthiebi.sgs.gradebook.service.TemplateVersionResolver templateVersionResolver;

    @Autowired
    private mthiebi.sgs.gradebook.service.PeriodTreeLoader periodTreeLoader;

    @Autowired
    private GradeEntryRepository gradeEntryRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PeriodRepository periodRepository;

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    private GuardianNotifier guardianNotifier;

    @PersistenceContext
    private EntityManager em;

    /**
     * A period and every period beneath it, in the class's own scheme.
     * <p>
     * Walked through the period tree rather than joined on parent_id, because
     * the gap is not always one level: publishing the year has to reach the
     * months two below it, past trimesters that hold no absence at all.
     */
    private List<Long> periodAndDescendants(ClassGroup classGroup, Long periodId) {
        return PeriodReach.of(periodTreeLoader.treeOf(classGroup.getPeriodScheme().getId()))
                .subtree(periodId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PublicationResult publish(Long classGroupId, Long periodId, Long subjectId,
                                     Long actorUserId) throws SGSException {
        return publish(classGroupId, periodId, subjectId, null, actorUserId);
    }

    /**
     * @param journalUuid restricts the release to one journal. Null keeps the
     *                    original behaviour - everything at that period - which
     *                    is what a class with a single journal wants and what
     *                    every existing caller relied on.
     */
    @Transactional(rollbackFor = Exception.class)
    public PublicationResult publish(Long classGroupId, Long periodId, Long subjectId,
                                     String journalUuid, Long actorUserId) throws SGSException {

        List<Enrollment> enrollments = enrollmentRepository.findActiveByClassGroup(classGroupId);
        if (enrollments.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "კლასში მოსწავლეები ვერ მოიძებნა");
        }
        ClassGroup classGroup = enrollments.get(0).getClassGroup();

        Period period = periodRepository.findById(periodId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "პერიოდი ვერ მოიძებნა"));
        if (!period.getScheme().getId().equals(classGroup.getPeriodScheme().getId())) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "პერიოდი არ ეკუთვნის კლასის სასწავლო სქემას");
        }

        // The period and everything under it. Publishing a trimester of grades
        // needs only the trimester, but the absence register's marks live on
        // days - so publishing the month has to reach them, or it releases
        // nothing and reports success.
        List<Long> periodIds = periodAndDescendants(classGroup, periodId);

        // Narrowed to one journal where the caller named one. Without it the
        // subtree walk above releases every journal's cells beneath the period -
        // publishing the absence register's year would release the whole class's
        // grades for the year and email every guardian about it.
        Long templateId = journalUuid == null || journalUuid.isEmpty() ? null
                : templateVersionResolver.journalByUuid(journalUuid).getId();

        List<GradeEntry> entries = gradeEntryRepository.findPublishableIn(
                classGroupId, periodIds, subjectId, templateId);

        Instant now = Instant.now();
        int released = 0;
        for (GradeEntry entry : entries) {
            if (isAlreadyCurrent(entry)) {
                continue;
            }
            entry.setPublishedValue(entry.getValue());
            entry.setPublishedSpecialValue(entry.getSpecialValue());
            entry.setPublishedAt(now);
            released++;
        }

        // Declared since phase 1 and never set by anything: once a version's
        // grades have gone to parents its shape must not change underneath
        // them, so the editor shows it as untouchable rather than inferring it
        // from a row count.
        //
        // Narrowed to the journal being published, which it was not before.
        // The subtree walk means publishing a period reaches every level under
        // it, so an unfiltered query locked the *other* journals' versions too -
        // and the monthly register publishes at the year, whose subtree is the
        // entire scheme. Releasing absence would have frozen the shape of the
        // grades journal, on a class that had published a single trimester mark.
        if (released > 0) {
            javax.persistence.TypedQuery<mthiebi.sgs.gradebook.model.TemplateVersion> query =
                    em.createQuery(
                                    "select distinct g.templateVersion from GradeEntry g "
                                            + "where g.enrollment.classGroup.id = :c and g.period.id in :p "
                                            + "  and g.publishedAt is not null "
                                            + "  and (:t is null or g.templateVersion.template.id = :t)",
                                    mthiebi.sgs.gradebook.model.TemplateVersion.class)
                            .setParameter("c", classGroupId)
                            .setParameter("p", periodIds)
                            .setParameter("t", templateId);
            for (mthiebi.sgs.gradebook.model.TemplateVersion version : query.getResultList()) {
                version.setStatus(mthiebi.sgs.gradebook.model.TemplateVersionStatus.LOCKED);
            }
        }

        Publication publication = new Publication();
        publication.setClassGroup(classGroup);
        publication.setPeriod(period);
        publication.setSubject(subjectId == null ? null : em.getReference(Subject.class, subjectId));
        publication.setPublishedAt(now);
        publication.setPublishedBy(actorUserId);
        publication.setCellCount(released);
        publicationRepository.save(publication);

        // Only for a journal that freezes on publish - which is to say, a real
        // one-time release. The register is republished as the month's hours
        // accumulate, so mailing on every publish would send every guardian in
        // the class a message several times a week, each announcing that
        // "grades" had been released, for a period label that is the academic
        // year. Dropping the lock made republication the ordinary path; the
        // notification had not been revisited to match.
        if (locksOnPublish(templateId)) {
            notifyAfterCommit(enrollments, classGroup, period, released);
        }

        return new PublicationResult(publication.getId(), released, entries.size(), now);
    }

    /**
     * Republishing is normal - it picks up marks entered since, and values whose
     * inputs have moved. A cell already showing what it would be given is left
     * alone so the stamp keeps meaning "when parents last saw a change".
     */
    private boolean isAlreadyCurrent(GradeEntry entry) {
        if (entry.getPublishedAt() == null) {
            return false;
        }
        boolean sameValue = entry.getValue() == null
                ? entry.getPublishedValue() == null
                : entry.getPublishedValue() != null
                && entry.getValue().compareTo(entry.getPublishedValue()) == 0;
        boolean sameSpecial = entry.getSpecialValue() == null
                ? entry.getPublishedSpecialValue() == null
                : entry.getSpecialValue().equals(entry.getPublishedSpecialValue());
        return sameValue && sameSpecial;
    }

    /**
     * Email after the transaction commits, never inside it.
     *
     * The legacy publish sent one message per student inline - roughly 900
     * synchronous sends in the request thread - so an SMTP timeout could roll
     * back a release that had, as far as anyone could tell, already happened.
     * Whether marks are visible must not depend on a mail server answering.
     */
    /**
     * Whether the journal being released freezes on publish.
     * <p>
     * A null templateId means the caller released everything at that period, in
     * which case grades are among it and the message is right.
     */
    private boolean locksOnPublish(Long templateId) {
        if (templateId == null) {
            return true;
        }
        mthiebi.sgs.gradebook.model.GradingTemplate journal =
                em.find(mthiebi.sgs.gradebook.model.GradingTemplate.class, templateId);
        return journal == null || journal.isLocksOnPublish();
    }

    private void notifyAfterCommit(List<Enrollment> enrollments, ClassGroup classGroup,
                                   Period period, int released) {
        if (released == 0) {
            return;
        }
        List<String> recipients = enrollments.stream()
                .map(e -> e.getStudent().getGuardianEmail())
                .filter(mail -> mail != null && mail.length() > 5)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        if (recipients.isEmpty()) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            guardianNotifier.notifyPublished(recipients, classGroup.getName(), period.getLabel());
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                guardianNotifier.notifyPublished(recipients,
                        classGroup.getName(), period.getLabel());
            }
        });
    }

    @Transactional(readOnly = true)
    /**
     * @param allowedClassGroupIds empty means unrestricted. Otherwise the log is
     *                             filtered to those classes: the console asks for
     *                             every class, so without this a class-scoped
     *                             user saw the whole school's release history.
     */
    public List<PublicationLogEntry> log(Long classGroupId, Set<Long> allowedClassGroupIds) {
        return publicationRepository.findLog(classGroupId).stream()
                .filter(p -> allowedClassGroupIds.isEmpty()
                        || allowedClassGroupIds.contains(p.getClassGroup().getId()))
                .map(p -> new PublicationLogEntry(
                        p.getId(),
                        p.getClassGroup().getName(),
                        p.getPeriod().getLabel(),
                        p.getSubject() == null ? null : p.getSubject().getName(),
                        p.getPublishedAt(),
                        p.getCellCount()))
                .collect(Collectors.toList());
    }
}
