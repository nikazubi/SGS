package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.GradeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GradeEntryRepository extends JpaRepository<GradeEntry, Long> {

    /**
     * The working set for one grid: every cell for these students, in this
     * subject, across the period subtree - plus the subject-less cells (ethics,
     * absence, rating) that sit alongside them.
     * <p>
     * One indexed seek, served by ix_grade_grid. Loading this up front is what
     * lets the engine evaluate without touching the database again, and what
     * removes the need for an upsert on the way back out: having seen the set,
     * we already know which cells exist.
     */
    @Query("select g from GradeEntry g "
            + "where g.enrollment.id in :enrollmentIds "
            + "  and g.period.id in :periodIds "
            + "  and (g.subject.id = :subjectId or g.subject is null)")
    List<GradeEntry> loadGrid(@Param("enrollmentIds") Collection<Long> enrollmentIds,
                              @Param("periodIds") Collection<Long> periodIds,
                              @Param("subjectId") Long subjectId);

    /**
     * Narrow companion query for cross-subject rules. A student-wide column such
     * as rating reads one component from every subject, so those cells have to
     * be present too - but only that component, not whole other grids.
     */
    @Query("select g from GradeEntry g "
            + "where g.enrollment.id in :enrollmentIds "
            + "  and g.period.id in :periodIds "
            + "  and g.component.id in :componentIds")
    List<GradeEntry> loadComponentsAcrossSubjects(@Param("enrollmentIds") Collection<Long> enrollmentIds,
                                                  @Param("periodIds") Collection<Long> periodIds,
                                                  @Param("componentIds") Collection<Long> componentIds);

    /**
     * Every published cell of one student, across every subject.
     * <p>
     * loadGrid cannot serve this: its subject arm is
     * `g.subject.id = :subjectId or g.subject is null`, so passing null returns
     * only the subject-less cells and silently drops every academic grade.
     */
    @Query("select g from GradeEntry g "
            + "left join fetch g.subject "
            + "where g.enrollment.id = :enrollmentId "
            + "  and g.period.id in :periodIds "
            + "  and g.publishedAt is not null")
    List<GradeEntry> loadPublishedForStudent(@Param("enrollmentId") Long enrollmentId,
                                             @Param("periodIds") Collection<Long> periodIds);

    /**
     * Everything a publish would release.
     * <p>
     * Blank cells are excluded deliberately: publishing one would stamp
     * published_at on a cell that never held a value, locking it against the
     * teacher who still has to fill it.
     * <p>
     * A subject filter narrows to that subject only - the subject-less
     * class-wide columns are not part of one subject's release.
     */
    @Query("select g from GradeEntry g "
            + "where g.enrollment.classGroup.id = :classGroupId "
            + "  and g.period.id = :periodId "
            + "  and (:subjectId is null or g.subject.id = :subjectId) "
            + "  and (g.value is not null or g.specialValue is not null "
            + "       or g.publishedAt is not null)")
    List<GradeEntry> findPublishable(@Param("classGroupId") Long classGroupId,
                                     @Param("periodId") Long periodId,
                                     @Param("subjectId") Long subjectId);

    /**
     * The same, over a period and everything beneath it.
     * <p>
     * A trimester's grades all live on the trimester, so one period was enough
     * until the absence register arrived: its marks live on *days*, and
     * publishing the month they belong to released nothing at all - the release
     * matched only cells whose period was the month itself.
     */
    @Query("select g from GradeEntry g "
            + "where g.enrollment.classGroup.id = :classGroupId "
            + "  and g.period.id in :periodIds "
            + "  and (:subjectId is null or g.subject.id = :subjectId) "
            + "  and (:templateId is null or g.templateVersion.template.id = :templateId) "
            + "  and (g.value is not null or g.specialValue is not null "
            + "       or g.publishedAt is not null)")
    List<GradeEntry> findPublishableIn(@Param("classGroupId") Long classGroupId,
                                       @Param("periodIds") List<Long> periodIds,
                                       @Param("subjectId") Long subjectId,
                                       @Param("templateId") Long templateId);

    /**
     * A class-wide matrix: one component across every subject.
     * <p>
     * The shape the monthly and annual exports need - students down, subjects
     * across - which the grid query cannot serve because it is scoped to one
     * subject.
     */
    @Query("select g from GradeEntry g "
            + "join fetch g.subject "
            + "where g.enrollment.classGroup.id = :classGroupId "
            + "  and g.period.id in :periodIds "
            + "  and g.component.code = :componentCode "
            + "  and g.templateVersion.template.id = :journalId "
            + "  and g.subject is not null")
    List<GradeEntry> loadMatrix(@Param("classGroupId") Long classGroupId,
                                @Param("periodIds") Collection<Long> periodIds,
                                @Param("componentCode") String componentCode,
                                @Param("journalId") Long journalId);

    /**
     * Which template version the marks in this period were entered against.
     * <p>
     * A period stays on the version it started under. Activating a new version
     * mid-year therefore applies to periods that have not begun, and never
     * silently re-renders marks that have already gone out - correcting an
     * October mark in February recomputes October's rules, not February's.
     * <p>
     * Moving a period onto a newer version is a deliberate act with a
     * recalculation attached, not a side effect of editing a cell.
     */
    @Query("select distinct g.templateVersion.id from GradeEntry g "
            + "where g.enrollment.classGroup.id = :classGroupId "
            + "  and g.period.id = :periodId "
            + "  and g.templateVersion.template.id = :templateId "
            + "  and (g.subject.id = :subjectId or g.subject is null)")
    List<Long> findTemplateVersionIdsInPeriod(@Param("classGroupId") Long classGroupId,
                                              @Param("periodId") Long periodId,
                                              @Param("subjectId") Long subjectId,
                                              @Param("templateId") Long templateId);
}
