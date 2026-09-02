package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.PostCategory;
import mthiebi.sgs.gradebook.model.PostImage;
import mthiebi.sgs.gradebook.model.PostKind;
import mthiebi.sgs.gradebook.service.content.CategoryService;
import mthiebi.sgs.gradebook.service.content.ImageService;
import mthiebi.sgs.gradebook.service.content.PostDraft;
import mthiebi.sgs.gradebook.service.content.PostService;
import mthiebi.sgs.gradebook.service.content.PostView;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * School news.
 * <p>
 * The one content module with **no class**, so the only one that does not go
 * through ClassScopeGuard — confirmed with the school. Scoping it by class would
 * mean a coordinator could only publish news to their own class, which is the
 * opposite of what school-wide means.
 */
@RestController
@RequestMapping("/api/gradebook/news")
public class NewsController {

    @Autowired
    private PostService postService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private ActorResolver actorResolver;

    @GetMapping
    @Secured({AuthConstants.MANAGE_NEWS})
    public List<PostView> list(@RequestParam(required = false) Long categoryId,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(required = false) Integer limit) {
        return postService.news(categoryId, from, to, limit);
    }

    @GetMapping("/{uuid}")
    @Secured({AuthConstants.MANAGE_NEWS})
    public PostView get(@PathVariable String uuid) throws SGSException {
        return require(uuid);
    }

    @PostMapping
    @Secured({AuthConstants.MANAGE_NEWS})
    public PostView save(@RequestBody PostDraft draft,
                         @RequestHeader("authorization") String authHeader) throws SGSException {
        if (draft.getUuid() != null && !draft.getUuid().isEmpty()) {
            require(draft.getUuid());
        }
        return postService.save(PostKind.NEWS, draft, actorResolver.idOf(authHeader));
    }

    @PostMapping("/{uuid}/publish")
    @Secured({AuthConstants.MANAGE_NEWS})
    public PostView publish(@PathVariable String uuid,
                            @RequestHeader("authorization") String authHeader)
            throws SGSException {
        require(uuid);
        return postService.publish(uuid, actorResolver.idOf(authHeader));
    }

    @PostMapping("/{uuid}/archive")
    @Secured({AuthConstants.MANAGE_NEWS})
    public void archive(@PathVariable String uuid,
                        @RequestParam(defaultValue = "true") boolean archived,
                        @RequestHeader("authorization") String authHeader) throws SGSException {
        require(uuid);
        postService.archive(uuid, archived, actorResolver.idOf(authHeader));
    }

    // ---- categories -------------------------------------------------------

    @GetMapping("/categories")
    @Secured({AuthConstants.MANAGE_NEWS})
    public List<CategoryOption> categories() {
        return categoryService.list().stream()
                .map(c -> new CategoryOption(c.getId(), c.getUuid(), c.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Find by name or create.
     * <p>
     * The autocomplete accepts something new without a separate management
     * screen, and a near-miss reuses the existing row rather than making a
     * second one - which is the whole point of categories being a table.
     */
    @PostMapping("/categories")
    @Secured({AuthConstants.MANAGE_NEWS})
    public CategoryOption addCategory(@RequestParam String name,
                                      @RequestHeader("authorization") String authHeader)
            throws SGSException {
        PostCategory saved = categoryService.findOrCreate(name, actorResolver.idOf(authHeader));
        return new CategoryOption(saved.getId(), saved.getUuid(), saved.getName());
    }

    // ---- images -----------------------------------------------------------

    /**
     * Upload one picture, downscaled and re-encoded on the way in.
     * <p>
     * Returns the uuid; the draft references it. Uploading and saving the post
     * are separate so a picture can be replaced without re-posting the article.
     */
    @PostMapping("/images")
    @Secured({AuthConstants.MANAGE_NEWS})
    public ImageResult upload(@RequestPart("file") MultipartFile file,
                              @RequestHeader("authorization") String authHeader)
            throws SGSException {
        try {
            PostImage stored = imageService.store(file.getBytes(),
                    actorResolver.idOf(authHeader));
            return new ImageResult(stored.getUuid(), stored.getByteSize(),
                    stored.getWidth(), stored.getHeight());
        } catch (IOException e) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "ფაილი ვერ წაიკითხა");
        }
    }

    /**
     * Serving the file.
     * <p>
     * The content type is what we wrote, not what was uploaded — the bytes were
     * re-encoded from decoded pixels, so this is the truth about them rather
     * than a claim the uploader made. Cached hard: the uuid changes when the
     * picture does.
     */
    @GetMapping("/images/{uuid}")
    @Secured({AuthConstants.MANAGE_NEWS})
    public ResponseEntity<byte[]> image(@PathVariable String uuid) throws SGSException {
        PostImage image = imageService.get(uuid);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(image.getBytes());
    }

    private PostView require(String uuid) throws SGSException {
        PostView view = postService.get(uuid);
        // No class check: news has no class. The kind check still matters, or
        // MANAGE_NEWS would edit a class's homework through a news URL.
        if (!PostKind.NEWS.name().equals(view.getKind())) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "ჩანაწერი სხვა ტიპისაა");
        }
        return view;
    }

    public static class CategoryOption {
        private final Long id;
        private final String uuid;
        private final String name;

        CategoryOption(Long id, String uuid, String name) {
            this.id = id;
            this.uuid = uuid;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }
    }

    public static class ImageResult {
        private final String uuid;
        private final int byteSize;
        private final int width;
        private final int height;

        ImageResult(String uuid, int byteSize, int width, int height) {
            this.uuid = uuid;
            this.byteSize = byteSize;
            this.width = width;
            this.height = height;
        }

        public String getUuid() {
            return uuid;
        }

        public int getByteSize() {
            return byteSize;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}
