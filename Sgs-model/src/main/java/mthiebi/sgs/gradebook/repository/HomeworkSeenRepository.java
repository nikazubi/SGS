package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.HomeworkSeen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeworkSeenRepository extends JpaRepository<HomeworkSeen, Long> {

    /**
     * Which of these posts this child has already opened.
     * <p>
     * Ids rather than entities: the caller only ever asks "is it in the set",
     * and the set is a month of homework.
     */
    @Query("select s.post.id from HomeworkSeen s "
            + "where s.enrollment.id = :enrollmentId and s.post.id in :postIds")
    List<Long> seenPostIds(@Param("enrollmentId") Long enrollmentId,
                           @Param("postIds") List<Long> postIds);
}
