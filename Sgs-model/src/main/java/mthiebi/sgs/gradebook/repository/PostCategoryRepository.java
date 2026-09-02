package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.PostCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostCategoryRepository extends JpaRepository<PostCategory, Long> {

    @Query("select c from PostCategory c where c.archived = false order by c.name")
    List<PostCategory> findActive();

    Optional<PostCategory> findByUuid(String uuid);

    /**
     * Matched case-insensitively so that typing a name that already exists
     * reuses it rather than being refused by the unique constraint - which is
     * the whole reason categories are a table and not free text.
     */
    @Query("select c from PostCategory c where lower(c.name) = lower(:name)")
    Optional<PostCategory> findByNameIgnoringCase(
            @org.springframework.data.repository.query.Param("name") String name);
}
