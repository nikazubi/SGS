package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    Optional<PostImage> findByUuid(String uuid);
}
