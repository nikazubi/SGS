package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.ChangeRequestStatus;
import mthiebi.sgs.gradebook.model.GradeChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeChangeRequestRepository extends JpaRepository<GradeChangeRequest, Long> {

    /**
     * The director's queue. Everything the screen shows is fetched here: the
     * legacy page issued a query per row for the student and the subject.
     */
    @Query("select r from GradeChangeRequest r "
            + "join fetch r.gradeEntry e "
            + "join fetch e.enrollment en "
            + "join fetch en.student "
            + "join fetch en.classGroup "
            + "join fetch e.component "
            + "join fetch e.period "
            + "left join fetch e.subject "
            + "where (:status is null or r.status = :status) "
            + "  and (:classGroupId is null or en.classGroup.id = :classGroupId) "
            + "order by r.requestedAt desc")
    List<GradeChangeRequest> findQueue(@Param("status") ChangeRequestStatus status,
                                       @Param("classGroupId") Long classGroupId);

    Optional<GradeChangeRequest> findByGradeEntryIdAndStatus(Long gradeEntryId,
                                                             ChangeRequestStatus status);

    /**
     * Which cells on a grid already have a request outstanding.
     */
    @Query("select r from GradeChangeRequest r "
            + "where r.status = mthiebi.sgs.gradebook.model.ChangeRequestStatus.PENDING "
            + "  and r.gradeEntry.id in :entryIds")
    List<GradeChangeRequest> findPendingForEntries(@Param("entryIds") List<Long> entryIds);
}
