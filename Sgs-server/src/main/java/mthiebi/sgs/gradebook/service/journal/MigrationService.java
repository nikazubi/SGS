package mthiebi.sgs.gradebook.service.journal;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.GradeComponent;
import mthiebi.sgs.gradebook.model.GradeEntry;
import mthiebi.sgs.gradebook.model.GradingTemplate;
import mthiebi.sgs.gradebook.model.TemplateVersion;
import mthiebi.sgs.gradebook.model.TemplateVersionStatus;
import mthiebi.sgs.gradebook.repository.GradeComponentRepository;
import mthiebi.sgs.gradebook.repository.GradingTemplateRepository;
import mthiebi.sgs.gradebook.repository.TemplateVersionRepository;
import mthiebi.sgs.gradebook.service.GradeWriteRequest;
import mthiebi.sgs.gradebook.service.GradeWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Moving a period onto a newer version of its journal.
 * <p>
 * Activation and migration are different acts. Activating a version reaches
 * future periods only; every period that already holds marks stays pinned to
 * whatever those marks were entered under. Bringing one forward is deliberate,
 * and it always recalculates - moving a period without recomputing would leave
 * values produced by the old rules sitting under the new columns, which is data
 * no rule explains.
 * <p>
 * So the choice offered is migrate-and-recalculate, or leave it alone.
 */
@Service
public class MigrationService {

    @Autowired
    private GradingTemplateRepository journalRepository;

    @Autowired
    private TemplateVersionRepository versionRepository;

    @Autowired
    private GradeComponentRepository componentRepository;

    @Autowired
    private GradeWriteService gradeWriteService;

    @PersistenceContext
    private EntityManager em;

    /**
     * What migrating would do, without doing it.
     * <p>
     * The same walk as the migration itself with the writes suppressed, so the
     * numbers in the prompt cannot disagree with what happens.
     */
    @Transactional(readOnly = true)
    public MigrationPlan preview(String journalUuid, Long classGroupId, Long periodId)
            throws SGSException {
        return plan(journalUuid, classGroupId, periodId);
    }

    /**
     * Every period of every class still on an older version.
     */
    @Transactional(readOnly = true)
    public MigrationPlan previewAll(String journalUuid) throws SGSException {
        GradingTemplate journal = journal(journalUuid);
        TemplateVersion target = activeVersionOf(journal);

        List<Object[]> stale = em.createQuery(
                        "select distinct g.enrollment.classGroup.id, g.period.id "
                                + "from GradeEntry g "
                                + "where g.templateVersion.template.id = :journalId "
                                + "  and g.templateVersion.id <> :targetId", Object[].class)
                .setParameter("journalId", journal.getId())
                .setParameter("targetId", target.getId())
                .getResultList();

        MigrationPlan combined = new MigrationPlan();
        combined.setJournalName(journal.getName());
        combined.setTargetVersionNo(target.getVersionNo());
        for (Object[] row : stale) {
            MigrationPlan one = plan(journalUuid, (Long) row[0], (Long) row[1]);
            combined.getScopes().addAll(one.getScopes());
            combined.setCellsToRecalculate(
                    combined.getCellsToRecalculate() + one.getCellsToRecalculate());
            combined.setMarksToDelete(combined.getMarksToDelete() + one.getMarksToDelete());
            combined.getRemovedColumns().addAll(one.getRemovedColumns());
        }
        return combined;
    }

    @Transactional(rollbackFor = Exception.class)
    public MigrationPlan migrate(String journalUuid, Long classGroupId, Long periodId,
                                 Long actorUserId) throws SGSException {

        MigrationPlan plan = plan(journalUuid, classGroupId, periodId);
        if (plan.getScopes().isEmpty()) {
            return plan;
        }

        GradingTemplate journal = journal(journalUuid);
        TemplateVersion target = activeVersionOf(journal);

        Set<String> targetCodes = componentRepository.findByTemplateVersion(target.getId()).stream()
                .map(GradeComponent::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<GradeEntry> entries = entriesIn(journal.getId(), classGroupId, periodId,
                target.getId());

        // A column the new version does not have has nowhere to put its marks.
        // Counted in the preview so nobody is surprised by it.
        List<GradeEntry> orphaned = entries.stream()
                .filter(e -> !targetCodes.contains(e.getComponent().getCode()))
                .collect(Collectors.toList());

        // A published mark is never deleted by a configuration change. There is
        // no unpublish, so removing one would retract a grade a parent has seen
        // with nothing to show them in its place.
        List<GradeEntry> published = orphaned.stream()
                .filter(GradeEntry::isPublished)
                .collect(Collectors.toList());
        if (!published.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "გადატანა შეუძლებელია: " + published.size()
                            + " გამოქვეყნებული შეფასება ეკუთვნის სვეტს, რომელიც "
                            + "ახალ ვერსიაში აღარ არსებობს");
        }
        // An open request would be orphaned with its cell.
        for (GradeEntry entry : orphaned) {
            em.createQuery("delete from GradeChangeRequest r where r.gradeEntry.id = :id")
                    .setParameter("id", entry.getId()).executeUpdate();
        }
        orphaned.forEach(em::remove);

        List<Long> subjectIds = new ArrayList<>();
        for (GradeEntry entry : entries) {
            if (orphaned.contains(entry)) {
                continue;
            }
            GradeComponent replacement = componentRepository
                    .findByTemplateVersion(target.getId()).stream()
                    .filter(c -> c.getCode().equals(entry.getComponent().getCode()))
                    .findFirst().orElse(null);
            if (replacement != null) {
                entry.setComponent(replacement);
                entry.setTemplateVersion(target);
            }
            Long subjectId = entry.getSubject() == null ? null : entry.getSubject().getId();
            if (!subjectIds.contains(subjectId)) {
                subjectIds.add(subjectId);
            }
        }
        em.flush();

        for (Long subjectId : subjectIds) {
            GradeWriteRequest request = new GradeWriteRequest();
            request.setJournalUuid(journalUuid);
            request.setClassGroupId(classGroupId);
            request.setSubjectId(subjectId);
            request.setPeriodId(periodId);
            gradeWriteService.recomputePeriod(request, actorUserId);
        }

        plan.setApplied(true);
        return plan;
    }

    @Transactional(rollbackFor = Exception.class)
    public MigrationPlan migrateAll(String journalUuid, Long actorUserId) throws SGSException {
        MigrationPlan preview = previewAll(journalUuid);
        for (MigrationScope scope : new ArrayList<>(preview.getScopes())) {
            migrate(journalUuid, scope.getClassGroupId(), scope.getPeriodId(), actorUserId);
        }
        preview.setApplied(true);
        return preview;
    }

    // ---- internals -------------------------------------------------------

    private MigrationPlan plan(String journalUuid, Long classGroupId, Long periodId)
            throws SGSException {

        GradingTemplate journal = journal(journalUuid);
        TemplateVersion target = activeVersionOf(journal);

        MigrationPlan plan = new MigrationPlan();
        plan.setJournalName(journal.getName());
        plan.setTargetVersionNo(target.getVersionNo());

        List<GradeEntry> entries = entriesIn(journal.getId(), classGroupId, periodId,
                target.getId());
        if (entries.isEmpty()) {
            return plan;
        }

        Set<String> targetCodes = componentRepository.findByTemplateVersion(target.getId()).stream()
                .map(GradeComponent::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        int deletable = 0;
        Set<String> removed = new LinkedHashSet<>();
        Set<Long> subjects = new LinkedHashSet<>();
        for (GradeEntry entry : entries) {
            if (!targetCodes.contains(entry.getComponent().getCode())) {
                deletable++;
                removed.add(entry.getComponent().getLabel());
            }
            if (entry.getSubject() != null) {
                subjects.add(entry.getSubject().getId());
            }
        }

        MigrationScope scope = new MigrationScope();
        scope.setClassGroupId(classGroupId);
        scope.setPeriodId(periodId);
        scope.setClassName(entries.get(0).getEnrollment().getClassGroup().getName());
        scope.setPeriodLabel(entries.get(0).getPeriod().getLabel());
        scope.setSubjectCount(Math.max(subjects.size(), 1));
        scope.setCellCount(entries.size());
        plan.getScopes().add(scope);

        plan.setCellsToRecalculate(entries.size() - deletable);
        plan.setMarksToDelete(deletable);
        plan.getRemovedColumns().addAll(removed);
        return plan;
    }

    private List<GradeEntry> entriesIn(Long journalId, Long classGroupId, Long periodId,
                                       Long targetVersionId) {
        return em.createQuery(
                        "select g from GradeEntry g "
                                + "join fetch g.component join fetch g.period "
                                + "join fetch g.enrollment e join fetch e.classGroup "
                                + "where e.classGroup.id = :classGroupId "
                                + "  and g.period.id = :periodId "
                                + "  and g.templateVersion.template.id = :journalId "
                                + "  and g.templateVersion.id <> :targetId", GradeEntry.class)
                .setParameter("classGroupId", classGroupId)
                .setParameter("periodId", periodId)
                .setParameter("journalId", journalId)
                .setParameter("targetId", targetVersionId)
                .getResultList();
    }

    private GradingTemplate journal(String uuid) throws SGSException {
        return journalRepository.findByUuid(uuid)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "ჟურნალი ვერ მოიძებნა: " + uuid));
    }

    private TemplateVersion activeVersionOf(GradingTemplate journal) throws SGSException {
        return versionRepository.findByTemplate(journal.getId()).stream()
                .filter(v -> v.getStatus() == TemplateVersionStatus.ACTIVE)
                .reduce((a, b) -> b)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "ჟურნალს არ აქვს აქტიური ვერსია"));
    }
}
