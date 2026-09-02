package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.Post;
import mthiebi.sgs.gradebook.model.PostKind;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByUuid(String uuid);

    /**
     * One subject's posts for a class, newest first.
     * <p>
     * Paged, because the list screen shows the newest few per subject and the
     * whole history only when someone asks for it. The accordion opens on a
     * class with a year of homework in it otherwise.
     * <p>
     * Archived rows are excluded: delete is soft, so they are still there.
     */
    @Query("select p from Post p "
            + "where p.kind = :kind and p.archived = false "
            + "and p.classGroup.id = :classGroupId "
            + "and (:subjectId is null or p.subject.id = :subjectId) "
            + "and (:from is null or p.eventDate >= :from) "
            + "and (:to is null or p.eventDate <= :to) "
            + "order by p.eventDate desc, p.id desc")
    List<Post> findForClass(@Param("kind") PostKind kind,
                            @Param("classGroupId") Long classGroupId,
                            @Param("subjectId") Long subjectId,
                            @Param("from") LocalDate from,
                            @Param("to") LocalDate to,
                            Pageable pageable);

    /**
     * The class's standing schedule or menu.
     * <p>
     * There is exactly one of each per class - the school enters it once for the
     * year and adjusts it - so this either finds it or the class has not made
     * one yet. Ordered by id so that if a duplicate ever appears, everyone
     * consistently edits the same one rather than whichever the database
     * happened to return.
     */
    @Query("select p from Post p where p.kind = :kind and p.archived = false "
            + "and p.classGroup.id = :classGroupId order by p.id")
    List<Post> findStanding(@Param("kind") PostKind kind,
                            @Param("classGroupId") Long classGroupId);

    /**
     * News, which belongs to no class.
     * <p>
     * Newest first by the date on the item, not by when it was written: the
     * school dates a post for when it is about.
     */
    @Query("select p from Post p where p.kind = :kind and p.archived = false "
            + "and (:categoryId is null or p.category.id = :categoryId) "
            + "and (:from is null or p.eventDate >= :from) "
            + "and (:to is null or p.eventDate <= :to) "
            + "order by p.eventDate desc, p.id desc")
    List<Post> findNews(@Param("kind") PostKind kind,
                        @Param("categoryId") Long categoryId,
                        @Param("from") LocalDate from,
                        @Param("to") LocalDate to,
                        Pageable pageable);

    /**
     * How many a subject has, so the list can say whether "see more" has
     * anything behind it rather than always offering it.
     */
    @Query("select count(p) from Post p "
            + "where p.kind = :kind and p.archived = false "
            + "and p.classGroup.id = :classGroupId "
            + "and (:subjectId is null or p.subject.id = :subjectId) "
            + "and (:from is null or p.eventDate >= :from) "
            + "and (:to is null or p.eventDate <= :to)")
    long countForClass(@Param("kind") PostKind kind,
                       @Param("classGroupId") Long classGroupId,
                       @Param("subjectId") Long subjectId,
                       @Param("from") LocalDate from,
                       @Param("to") LocalDate to);

    // ---- the parent side ---------------------------------------------------
    //
    // Separate queries rather than more parameters on the staff ones. Two
    // conditions apply here and never there, and both are the kind that must
    // not be possible to omit by passing null: only PUBLISHED posts, and only
    // work this particular child was given.

    /**
     * A child's homework between two dates, newest first.
     * <p>
     * The targeting clause is the point. A post with no targets is for the whole
     * class; a post with targets is for exactly those children. Written as "no
     * targets OR I am one of them", so adding the first target to a post
     * silently narrows it rather than leaving it visible to everyone.
     * <p>
     * Only PUBLISHED. A draft is work the teacher has not sent, and the staff
     * console shows drafts in a different colour precisely because they are not
     * supposed to have left the building.
     */
    @Query("select p from Post p "
            + "where p.kind = mthiebi.sgs.gradebook.model.PostKind.HOMEWORK "
            + "  and p.archived = false "
            + "  and p.status = mthiebi.sgs.gradebook.model.PostStatus.PUBLISHED "
            + "  and p.classGroup.id = :classGroupId "
            + "  and p.eventDate between :from and :to "
            + "  and (p.targets is empty or :enrollmentId in "
            + "        (select t.enrollment.id from PostTarget t where t.post.id = p.id)) "
            + "order by p.eventDate desc, p.subject.name, p.id")
    List<Post> findHomeworkForChild(@Param("classGroupId") Long classGroupId,
                                    @Param("enrollmentId") Long enrollmentId,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);

    /**
     * Published news, newest first.
     * <p>
     * No class and no school: the school confirmed news is institution-wide and
     * every parent sees every item. The category is a label to filter by, not a
     * visibility rule.
     */
    @Query("select p from Post p "
            + "where p.kind = mthiebi.sgs.gradebook.model.PostKind.NEWS "
            + "  and p.archived = false "
            + "  and p.status = mthiebi.sgs.gradebook.model.PostStatus.PUBLISHED "
            + "  and (:categoryId is null or p.category.id = :categoryId) "
            + "order by p.eventDate desc, p.id desc")
    List<Post> findPublishedNews(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("select count(p) from Post p "
            + "where p.kind = mthiebi.sgs.gradebook.model.PostKind.NEWS "
            + "  and p.archived = false "
            + "  and p.status = mthiebi.sgs.gradebook.model.PostStatus.PUBLISHED "
            + "  and (:categoryId is null or p.category.id = :categoryId)")
    long countPublishedNews(@Param("categoryId") Long categoryId);

    /**
     * The class's published schedule or menu.
     * <p>
     * There is exactly one of each per class - the school enters it once for the
     * year and adjusts it - so this either finds it or the class has not made
     * one yet. Ordered by id so that if a duplicate ever appears, every parent
     * consistently reads the same one.
     */
    @Query("select p from Post p "
            + "where p.kind = :kind and p.archived = false "
            + "  and p.status = mthiebi.sgs.gradebook.model.PostStatus.PUBLISHED "
            + "  and p.classGroup.id = :classGroupId "
            + "order by p.id")
    List<Post> findPublishedStanding(@Param("kind") PostKind kind,
                                     @Param("classGroupId") Long classGroupId);

    /**
     * The characterizations written about one child, newest first.
     * <p>
     * Targeted, and unlike homework an untargeted one reaches nobody: a
     * characterization is about a named student by definition, so one with no
     * target is a draft someone has not finished addressing rather than a note
     * to the whole class.
     */
    @Query("select p from Post p "
            + "where p.kind = mthiebi.sgs.gradebook.model.PostKind.CHARACTERIZATION "
            + "  and p.archived = false "
            + "  and p.status = mthiebi.sgs.gradebook.model.PostStatus.PUBLISHED "
            + "  and :enrollmentId in "
            + "        (select t.enrollment.id from PostTarget t where t.post.id = p.id) "
            + "order by p.eventDate desc, p.id desc")
    List<Post> findCharacterizationsForChild(@Param("enrollmentId") Long enrollmentId);
}
