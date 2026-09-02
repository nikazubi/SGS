package mthiebi.sgs.gradebook.service;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.engine.CellKey;
import mthiebi.sgs.gradebook.engine.CellValue;
import mthiebi.sgs.gradebook.engine.ComponentDef;
import mthiebi.sgs.gradebook.engine.EvaluationContext;
import mthiebi.sgs.gradebook.engine.PeriodReach;
import mthiebi.sgs.gradebook.engine.PeriodTree;
import mthiebi.sgs.gradebook.engine.RecomputeEngine;
import mthiebi.sgs.gradebook.engine.TemplateGraph;
import mthiebi.sgs.gradebook.engine.WorkingSet;
import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.model.GradeComponent;
import mthiebi.sgs.gradebook.model.GradeEntry;
import mthiebi.sgs.gradebook.model.GradeSource;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.model.Subject;
import mthiebi.sgs.gradebook.model.TemplateVersion;
import mthiebi.sgs.gradebook.repository.EnrollmentRepository;
import mthiebi.sgs.gradebook.repository.GradeEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Applies a batch of grade edits and everything that follows from them.
 * <p>
 * The shape of this is the answer to why the old console felt slow. It resolved
 * the student, the class and the subject on every single cell, searched an
 * unindexed heap for an existing row, and then had the browser reload the whole
 * grid afterwards - roughly nine round trips per cell plus a full refetch.
 * <p>
 * Here the context is resolved once, the working set is read in one indexed
 * query, evaluation happens entirely in memory, and the recomputed values ride
 * home on the response so the client patches rather than reloads.
 */
@Service
public class GradeWriteService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private GradeEntryRepository gradeEntryRepository;

    @Autowired
    private TemplateVersionResolver templateVersionResolver;

    @Autowired
    private TemplateGraphLoader templateGraphLoader;

    @Autowired
    private PeriodTreeLoader periodTreeLoader;

    @Autowired
    private SpecialValueRegistry specialValueRegistry;

    private final RecomputeEngine engine = new RecomputeEngine();

    @PersistenceContext
    private EntityManager em;

    @Transactional(rollbackFor = Exception.class)
    public GradeWriteResult apply(GradeWriteRequest request, Long actorUserId) throws SGSException {
        return apply(request, actorUserId, true);
    }

    /**
     * The same write, with the publication lock lifted.
     * <p>
     * Only for applying a change request the director has approved: that is the
     * one path where changing a published cell is the point. Deliberately not
     * reachable from the controller and not a flag on the request DTO - a
     * bypass that travels in the request body is a bypass anyone can ask for.
     */
    @Transactional(rollbackFor = Exception.class)
    public GradeWriteResult applyApproved(GradeWriteRequest request, Long actorUserId)
            throws SGSException {
        return apply(request, actorUserId, false);
    }

    /**
     * Recompute every calculated cell of a period from its inputs.
     * <p>
     * Used after a period is migrated onto a new version: the stored derived
     * values were produced by the old rules, and the new ones may not even have
     * the same columns. Seeds with the marks a person typed and lets the engine
     * propagate, so an override typed over a formula survives - overrides are
     * sticky, and a configuration change must not quietly revert one.
     */
    @Transactional(rollbackFor = Exception.class)
    public GradeWriteResult recomputePeriod(GradeWriteRequest request, Long actorUserId)
            throws SGSException {
        GradeWriteRequest seedAll = new GradeWriteRequest();
        seedAll.setJournalUuid(request.getJournalUuid());
        seedAll.setClassGroupId(request.getClassGroupId());
        seedAll.setSubjectId(request.getSubjectId());
        seedAll.setPeriodId(request.getPeriodId());
        seedAll.setEntries(java.util.Collections.emptyList());
        return apply(seedAll, actorUserId, false, true);
    }

    private GradeWriteResult apply(GradeWriteRequest request, Long actorUserId,
                                   boolean enforcePublicationLock) throws SGSException {
        return apply(request, actorUserId, enforcePublicationLock, false);
    }

    private GradeWriteResult apply(GradeWriteRequest request, Long actorUserId,
                                   boolean enforcePublicationLock,
                                   boolean recomputeEverything) throws SGSException {
        GradeWriteResult result = new GradeWriteResult();
        if (!recomputeEverything
                && (request.getEntries() == null || request.getEntries().isEmpty())) {
            return result;
        }

        // ---- context, resolved once for the whole batch -------------------
        List<Enrollment> enrollments = enrollmentRepository.findActiveByClassGroup(request.getClassGroupId());
        if (enrollments.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "კლასში მოსწავლეები ვერ მოიძებნა");
        }
        ClassGroup classGroup = enrollments.get(0).getClassGroup();

        TemplateVersion version = templateVersionResolver.resolve(
                        request.getClassGroupId(), request.getSubjectId(), request.getPeriodId(),
                        templateVersionResolver.journalByUuid(request.getJournalUuid()).getId())
                .getVersion();
        TemplateGraph graph = templateGraphLoader.graphOf(version.getId());
        PeriodTree periods = periodTreeLoader.treeOf(classGroup.getPeriodScheme().getId());

        Map<Long, Enrollment> enrollmentsById = enrollments.stream()
                .collect(Collectors.toMap(Enrollment::getId, e -> e));
        List<Long> enrollmentIds = new ArrayList<>(enrollmentsById.keySet());

        // ---- working set, read once ---------------------------------------
        List<Long> periodIds = relevantPeriods(periods, request.getPeriodId(), graph);
        Map<CellKey, GradeEntry> existing = loadWorkingSet(
                enrollmentIds, periodIds, request.getSubjectId(), graph, version.getId());

        WorkingSet workingSet = new WorkingSet(existing.entrySet().stream()
                .collect(HashMap::new,
                        (m, e) -> m.put(e.getKey(), toCellValue(e.getValue())),
                        HashMap::putAll));

        EvaluationContext ctx = new EvaluationContext(graph, periods, workingSet,
                subjectIdsOf(classGroup), specialValueRegistry.behavioursFor(version.getId()));
        ctx.setJournalDepth(version.getTemplate().getFrequency().getDepth());
        boolean journalLocks = version.getTemplate().isLocksOnPublish();

        // ---- apply the manual edits ---------------------------------------
        Set<CellKey> seeds = new LinkedHashSet<>();
        Set<CellKey> releasedOverrides = new LinkedHashSet<>();

        for (GradeEntryUpdate update : request.getEntries()) {
            ComponentDef component = graph.byCode(update.getComponentCode());
            if (component == null) {
                throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "უცნობი სვეტი: " + update.getComponentCode());
            }

            CellKey key = new CellKey(update.getEnrollmentId(),
                    component.isSubjectScoped() ? request.getSubjectId() : null,
                    request.getPeriodId(),
                    component.getId());

            GradeEntry current = existing.get(key);
            if (isConflicting(update, current)) {
                result.getConflicts().add(reject(update, current,
                        CellRejectionReason.VERSION_CONFLICT));
                continue;
            }

            // Published cells are read-only, for a journal that locks. Only
            // direct edits are blocked - recomputation still writes through, so
            // a published derived cell may diverge from what parents were shown,
            // which is precisely the state a change request exists to resolve.
            //
            // The register does not lock: its month accumulates and is
            // republished as it fills, so an approval per top-up would be an
            // approval on the normal path. See GradingTemplate.locksOnPublish.
            if (enforcePublicationLock && journalLocks && current != null && current.isPublished()) {
                result.getConflicts().add(reject(update, current, CellRejectionReason.PUBLISHED));
                continue;
            }

            CellRejectionReason invalid = validate(update, component);
            if (invalid != null) {
                result.getConflicts().add(reject(update, current, invalid));
                continue;
            }

            boolean override = Boolean.TRUE.equals(update.getOverride())
                    && component.isDerived() && component.isAllowOverride();

            if (component.isDerived() && !override && !component.isAllowOverride()) {
                result.getConflicts().add(reject(update, current,
                        CellRejectionReason.NOT_EDITABLE));
                continue;
            }

            boolean wasOverridden = current != null && current.isOverride();
            if (wasOverridden && !override) {
                releasedOverrides.add(key);
            }

            workingSet.put(key, new CellValue(update.getValue(), update.getSpecialValue(),
                    component.isDerived() ? GradeSource.DERIVED : GradeSource.MANUAL, override, 0));
            seeds.add(key);
        }

        // A full recompute seeds with everything already stored, so the engine
        // walks the whole graph rather than only what an edit touched.
        if (recomputeEverything) {
            seeds.addAll(existing.keySet());
            // Passed as alsoEvaluate, not only as seeds: recompute evaluates
            // what *depends* on a seed, so a cell that was manual under the old
            // version and calculated under the new one would otherwise keep its
            // old value - exactly the state a migration exists to remove.
            releasedOverrides.addAll(existing.keySet());
        }

        // ---- propagate ------------------------------------------------------
        engine.recompute(seeds, releasedOverrides, ctx);

        // ---- persist --------------------------------------------------------
        Map<CellResult, GradeEntry> writtenRows = new java.util.LinkedHashMap<>();
        persist(workingSet, existing, enrollmentsById, request, version, graph, actorUserId,
                result, seeds, templateGraphLoader.versionIdByComponent(version.getId()),
                writtenRows);

        // @Version is bumped by the flush, not by save(), so reading it before
        // this point hands the client the pre-increment number - and the next
        // edit of the same cell then looks like a competing write.
        em.flush();
        // The row version is read after the flush, because @Version is bumped
        // there and not by save() - reporting it earlier handed the client a
        // number the database had already moved past, and the next edit of the
        // same cell then looked like a competing write.
        writtenRows.forEach((cell, entry) -> cell.setRowVersion(entry.getRowVersion()));
        return result;
    }

    /**
     * Which version already owns cells at this period, if any.
     */
    private Long versionAlreadyAt(CellKey key, Map<Long, Enrollment> enrollmentsById) {
        Enrollment enrollment = enrollmentsById.get(key.getEnrollmentId());
        if (enrollment == null) {
            return null;
        }
        List<Long> found = em.createQuery(
                        "select distinct g.templateVersion.id from GradeEntry g "
                                + "where g.enrollment.classGroup.id = :c and g.period.id = :p "
                                + "  and g.component.code = :code", Long.class)
                .setParameter("c", enrollment.getClassGroup().getId())
                .setParameter("p", key.getPeriodId())
                .setParameter("code", componentCodeOf(key.getComponentId()))
                .setMaxResults(1)
                .getResultList();
        return found.isEmpty() ? null : found.get(0);
    }

    private String componentCodeOf(Long componentId) {
        return em.find(GradeComponent.class, componentId).getCode();
    }

    private GradeComponent componentAt(Long versionId, String code) {
        List<GradeComponent> found = em.createQuery(
                        "select c from GradeComponent c "
                                + "where c.templateVersion.id = :v and c.code = :code",
                        GradeComponent.class)
                .setParameter("v", versionId).setParameter("code", code)
                .getResultList();
        return found.isEmpty() ? null : found.get(0);
    }

    private CellConflict reject(GradeEntryUpdate update, GradeEntry current,
                                CellRejectionReason reason) {
        return new CellConflict(update.getEnrollmentId(), update.getComponentCode(),
                update.getValue(),
                current == null ? null : current.getValue(),
                current == null ? null : current.getRowVersion(),
                reason);
    }

    /**
     * Whether the value is one this column can hold.
     * <p>
     * Nothing checked this before, so 999 on a 0-10 column persisted and flowed
     * into every average built on it. A special code that the template does not
     * declare is refused for the same reason: it would aggregate by whatever
     * the engine assumes rather than by what the school configured.
     */
    private CellRejectionReason validate(GradeEntryUpdate update, ComponentDef component) {
        String special = update.getSpecialValue();
        if (special != null && !special.isEmpty()) {
            if (!component.isAllowSpecialValues()
                    || !specialValueRegistry.isKnown(special)) {
                return CellRejectionReason.INVALID_VALUE;
            }
            return null;
        }
        java.math.BigDecimal value = update.getValue();
        if (value == null) {
            return null;
        }
        if (component.getScaleMin() != null && value.compareTo(component.getScaleMin()) < 0) {
            return CellRejectionReason.OUT_OF_RANGE;
        }
        if (component.getScaleMax() != null && value.compareTo(component.getScaleMax()) > 0) {
            return CellRejectionReason.OUT_OF_RANGE;
        }
        return null;
    }

    private boolean isConflicting(GradeEntryUpdate update, GradeEntry current) {
        return update.getExpectedVersion() != null
                && current != null
                && current.getRowVersion() != update.getExpectedVersion();
    }

    /**
     * Every period a write can reach.
     * <p>
     * Deferred to {@link PeriodReach#workingSet}, which is also what the explain
     * trace loads. They were two separate expressions until this refactor, and
     * the difference was not cosmetic: only this one learned about DESCENDANTS,
     * so an explain of a yearly absence total resolved sources the write path
     * had actually summed and reported them as empty.
     * <p>
     * Why it matters that the set is big enough: a rule evaluated against a
     * working set missing some of its inputs does not fail. It computes a total
     * from the fraction that is loaded and stores it. A mark in October would
     * overwrite the year's count with October's alone, silently.
     */
    private List<Long> relevantPeriods(PeriodTree periods, Long periodId, TemplateGraph graph) {
        return PeriodReach.of(periods).workingSet(periodId, graph);
    }

    private Map<CellKey, GradeEntry> loadWorkingSet(List<Long> enrollmentIds, List<Long> periodIds,
                                                    Long subjectId, TemplateGraph graph,
                                                    Long versionId) {
        Map<CellKey, GradeEntry> byKey = new HashMap<>();
        for (GradeEntry entry : gradeEntryRepository.loadGrid(enrollmentIds, periodIds, subjectId)) {
            byKey.put(keyOf(entry), entry);
        }

        // Student-wide columns read one component from every subject, so those
        // cells must be present too - but only that component, not whole grids.
        List<Long> crossSubject = crossSubjectSourceIds(graph, versionId);
        if (!crossSubject.isEmpty()) {
            for (GradeEntry entry : gradeEntryRepository
                    .loadComponentsAcrossSubjects(enrollmentIds, periodIds, crossSubject)) {
                byKey.putIfAbsent(keyOf(entry), entry);
            }
        }
        return byKey;
    }

    /**
     * Columns the grid query will not have loaded.
     * <p>
     * Two cases, both served by the same narrow query. A student-wide column
     * such as rating reads one component from every subject; and a
     * cross-journal formula reads a column belonging to another journal
     * entirely, which the subject-scoped grid query never sees.
     */
    private List<Long> crossSubjectSourceIds(TemplateGraph graph, Long versionId) {
        java.util.Map<Long, Long> owner = templateGraphLoader.versionIdByComponent(versionId);
        Set<Long> ids = new LinkedHashSet<>();
        for (ComponentDef component : graph.all()) {
            if (!component.isDerived()) {
                continue;
            }
            boolean studentWide = !component.isSubjectScoped();
            for (Long sourceId : component.getRule().allSourceComponentIds()) {
                Long sourceVersion = owner.get(sourceId);
                boolean otherJournal = sourceVersion != null && !sourceVersion.equals(versionId);
                if (studentWide || otherJournal) {
                    ids.add(sourceId);
                }
            }
        }
        // The other journal's own columns have to come too, or a formula that
        // reads a calculated column there would see it as empty.
        for (ComponentDef component : graph.all()) {
            Long componentVersion = owner.get(component.getId());
            if (componentVersion != null && !componentVersion.equals(versionId)) {
                ids.add(component.getId());
            }
        }
        return new ArrayList<>(ids);
    }

    /**
     * Re-reads the cells this batch is about to create, and adopts any that
     * another request created in the meantime.
     * <p>
     * One query for the whole batch, and only when there is something to
     * insert. An adopted row is updated rather than inserted, so the value the
     * request carries still lands - which is what the writer asked for, and
     * better than reporting a conflict for a cell they were only ever adding to.
     */
    private void refreshInsertsFromDatabase(WorkingSet workingSet,
                                            Map<CellKey, GradeEntry> existing,
                                            GradeWriteRequest request,
                                            Map<Long, Enrollment> enrollmentsById) {
        Set<Long> periodIds = new LinkedHashSet<>();
        for (CellKey key : workingSet.changed().keySet()) {
            if (!existing.containsKey(key)) {
                periodIds.add(key.getPeriodId());
            }
        }
        if (periodIds.isEmpty()) {
            return;
        }
        for (GradeEntry entry : gradeEntryRepository.loadGrid(
                new ArrayList<>(enrollmentsById.keySet()),
                new ArrayList<>(periodIds), request.getSubjectId())) {
            existing.putIfAbsent(keyOf(entry), entry);
        }
    }

    private List<Long> subjectIdsOf(ClassGroup classGroup) {
        return em.createQuery(
                "select cs.subject.id from ClassSubject cs where cs.classGroup.id = :id order by cs.sortIndex",
                Long.class).setParameter("id", classGroup.getId()).getResultList();
    }

    private void persist(WorkingSet workingSet,
                         Map<CellKey, GradeEntry> existing,
                         Map<Long, Enrollment> enrollmentsById,
                         GradeWriteRequest request,
                         TemplateVersion version,
                         TemplateGraph graph,
                         Long actorUserId,
                         GradeWriteResult result,
                         Set<CellKey> seeds,
                         java.util.Map<Long, Long> versionByComponent,
                         Map<CellResult, GradeEntry> writtenRows) {

        // Everything about to be inserted, re-read once.
        //
        // The working set was loaded at the top of the request; anything created
        // since then is invisible to it, and inserting over it violates
        // uq_grade_cell. That matters more than a lost cell: the violation
        // surfaces at flush, and Hibernate marks the whole transaction
        // rollback-only when it converts one - so a per-cell catch cannot
        // recover, every later flush in this loop re-throws, and the batch dies
        // at commit. One query closes the window that a catch could not.
        refreshInsertsFromDatabase(workingSet, existing, request, enrollmentsById);

        for (Map.Entry<CellKey, CellValue> change : workingSet.changed().entrySet()) {
            CellKey key = change.getKey();
            CellValue value = change.getValue();

            GradeEntry entry = existing.get(key);
            if (entry == null) {
                entry = new GradeEntry();
                entry.setEnrollment(enrollmentsById.get(key.getEnrollmentId()));
                entry.setSubject(key.getSubjectId() == null
                        ? null : em.getReference(Subject.class, key.getSubjectId()));
                entry.setPeriod(em.getReference(Period.class, key.getPeriodId()));
                entry.setComponent(em.getReference(GradeComponent.class, key.getComponentId()));
                // A cell recomputed through a cross-journal formula belongs to
                // the other journal's version, not to the one being written -
                // stamping this version on it would make that period look like
                // it holds two versions and jam the resolver.
                Long owner = versionByComponent.get(key.getComponentId());
                entry.setTemplateVersion(owner == null || owner.equals(version.getId())
                        ? version
                        : em.getReference(TemplateVersion.class, owner));

                // A rollup at a period above the one being written - an annual
                // grade fed by trimesters - is reached from every trimester.
                // Under a mid-year activation those trimesters resolve to
                // different versions, so it would be written twice under
                // different component ids, and the resolver would then see two
                // versions in that period and refuse it forever. It takes the
                // version already present there instead.
                Long settled = versionAlreadyAt(key, enrollmentsById);
                if (settled != null && !settled.equals(entry.getTemplateVersion().getId())) {
                    entry.setTemplateVersion(em.getReference(TemplateVersion.class, settled));
                    GradeComponent sameCode = componentAt(settled,
                            graph.byId(key.getComponentId()).getCode());
                    if (sameCode != null) {
                        entry.setComponent(sameCode);
                    }
                }
                entry.setCreatedBy(actorUserId);
            }

            entry.setValue(value.getValue());
            entry.setSpecialValue(value.getSpecialValue());
            entry.setSource(value.getSource());
            entry.setOverride(value.isOverride());
            entry.setUpdatedBy(actorUserId);

            // No catch here, deliberately.
            //
            // There used to be one, reporting a uq_grade_cell violation as a
            // per-cell conflict and continuing. It could never work: converting a
            // constraint violation marks the transaction rollback-only, so the
            // `continue` ran on a dead session, every later flush re-threw, and the
            // commit failed anyway - a 500 dressed up as graceful degradation.
            // Recovering per cell needs a transaction per cell, which is what
            // DailyAbsenceWriter does and what this loop, building each entry from
            // session-bound references, cannot cheaply be given.
            //
            // The refresh above removes the realistic cause. What is left is two
            // requests inserting the same cell within one query of each other, and
            // that fails the batch loudly rather than quietly - the client refetches
            // and sees the winner's value, which is the correct state.
            GradeEntry saved = gradeEntryRepository.save(entry);
            existing.put(key, saved);

            CellResult cell = new CellResult(key.getEnrollmentId(), key.getSubjectId(),
                    key.getPeriodId(), graph.byId(key.getComponentId()).getCode(),
                    saved.getValue(), saved.getSpecialValue(), saved.getRowVersion());

            // Paired with the row it came from, so the version can be read
            // back after the flush without matching on a code that two linked
            // journals may share.
            writtenRows.put(cell, saved);

            if (seeds.contains(key)) {
                result.getApplied().add(cell);
            } else {
                result.getDerived().add(cell);
            }
        }
    }

    private CellKey keyOf(GradeEntry entry) {
        return new CellKey(entry.getEnrollment().getId(),
                entry.getSubject() == null ? null : entry.getSubject().getId(),
                entry.getPeriod().getId(),
                entry.getComponent().getId());
    }

    private CellValue toCellValue(GradeEntry entry) {
        return new CellValue(entry.getValue(), entry.getSpecialValue(), entry.getSource(),
                entry.isOverride(), entry.getRowVersion());
    }

}
