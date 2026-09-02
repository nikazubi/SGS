package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.Publication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, Long> {

    /**
     * The release log, newest first - what the close-period screen lists.
     */
    @Query("select p from Publication p "
            + "join fetch p.classGroup join fetch p.period "
            + "where (:classGroupId is null or p.classGroup.id = :classGroupId) "
            + "  and p.fromChangeRequest = false "
            + "order by p.publishedAt desc")
    List<Publication> findLog(@Param("classGroupId") Long classGroupId);
}
