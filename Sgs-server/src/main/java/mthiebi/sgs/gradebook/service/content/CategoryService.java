package mthiebi.sgs.gradebook.service.content;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.PostCategory;
import mthiebi.sgs.gradebook.repository.PostCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * News categories.
 * <p>
 * The school said it did not mind whether these were free text or configured.
 * They are a table behind an autocomplete: typing feels the same, and
 * "საბავშვო ბაღი" cannot become two categories because someone left a double
 * space in one of them.
 */
@Service
public class CategoryService {

    @Autowired
    private PostCategoryRepository postCategoryRepository;

    @Transactional(readOnly = true)
    public List<PostCategory> list() {
        return postCategoryRepository.findActive();
    }

    /**
     * Find by name or create, so the autocomplete can accept something new
     * without a separate "manage categories" screen.
     * <p>
     * Matched case-insensitively and trimmed: the point of a table here is that
     * a near-miss reuses the existing row rather than making a second one.
     */
    @Transactional(rollbackFor = Exception.class)
    public PostCategory findOrCreate(String name, Long actorId) throws SGSException {
        if (name == null || name.trim().isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "კატეგორიას სახელი სჭირდება");
        }
        String trimmed = name.trim();
        return postCategoryRepository.findByNameIgnoringCase(trimmed)
                .orElseGet(() -> {
                    PostCategory created = new PostCategory();
                    created.setUuid(UUID.randomUUID().toString());
                    created.setName(trimmed);
                    created.setCreatedBy(actorId);
                    created.setUpdatedBy(actorId);
                    return postCategoryRepository.save(created);
                });
    }
}
