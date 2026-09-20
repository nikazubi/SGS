package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.gradebook.model.PeriodKind;
import mthiebi.sgs.gradebook.service.parent.*;
import mthiebi.sgs.utils.UtilsJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The parent portal.
 * <p>
 * Read-only, and scoped to the student the token belongs to - the identity
 * comes from the token, never from a request parameter, so one login cannot ask
 * about another child.
 * <p>
 * Every value served here is the published one. Reading the working column
 * would show parents work in progress and undo the flow publication exists for.
 */
@RestController
@RequestMapping("/api/parent")
public class ParentController {

    @Autowired
    private ParentViewService parentViewService;

    @Autowired
    private UtilsJwt utilsJwt;

    @Autowired
    private mthiebi.sgs.gradebook.service.parent.ParentContentService parentContentService;

    @Autowired
    private mthiebi.sgs.gradebook.service.content.CategoryService categoryService;

    @Autowired
    private mthiebi.sgs.gradebook.service.content.ImageService imageService;

    /**
     * The boxes on the landing page - the journals the school has released, and
     * that this child's school shows at all. Primary gets none.
     */
    @GetMapping("/journals")
    public List<ParentJournal> journals(@RequestHeader("authorization") String authHeader)
            throws Exception {
        return parentViewService.journals(studentOf(authHeader));
    }

    /**
     * The buttons the landing page draws.
     *
     * <p>Not the same as the journals above, and that is the point: three of the
     * school's five parent screens are the trimester journal at different
     * settings, so a button is a configured entry point rather than a journal.
     * A journal nobody has configured stands in for itself, so this is a
     * superset of {@code /journals} and the older endpoint stays for the primary
     * grid, which asks a different question - which journal is the register.
     */
    @GetMapping("/views")
    public List<ParentLandingView> views(@RequestHeader("authorization") String authHeader)
            throws Exception {
        return parentViewService.views(studentOf(authHeader));
    }

    // ---- homework -----------------------------------------------------------

    /**
     * A month of the calendar: which days hold work, and how much is unopened.
     */
    @GetMapping("/homework")
    public ParentContentView.HomeworkMonth homeworkMonth(
            @RequestHeader("authorization") String authHeader,
            @RequestParam String month) throws Exception {
        return parentContentService.homeworkMonth(studentOf(authHeader), month);
    }

    /**
     * One day, grouped by subject. Reading it marks nothing.
     */
    @GetMapping("/homework/{date}")
    public ParentContentView.HomeworkDayDetail homeworkDay(
            @RequestHeader("authorization") String authHeader,
            @PathVariable String date) throws Exception {
        return parentContentService.homeworkDay(studentOf(authHeader), date);
    }

    /**
     * Records that the parent has opened these assignments.
     * <p>
     * A batch, because the console debounces rather than writing on every tap.
     * Idempotent, so a retry after a dropped response costs nothing.
     */
    @PostMapping("/homework/seen")
    public int markSeen(@RequestHeader("authorization") String authHeader,
                        @RequestBody SeenRequest request) throws Exception {
        return parentContentService.markSeen(studentOf(authHeader), request.getPostUuids());
    }

    // ---- schedule, menu, the child's description ----------------------------
    //
    // Primary-school modules. Not gated here beyond the token: a basic-school
    // parent who guessed the URL would get their own class's schedule, which is
    // their own child's information and simply not offered to them. What decides
    // whether the box appears is /modules.

    @GetMapping("/schedule")
    public ParentContentView.StandingDoc schedule(
            @RequestHeader("authorization") String authHeader) throws Exception {
        return parentContentService.standingDoc(studentOf(authHeader),
                mthiebi.sgs.gradebook.model.PostKind.SCHEDULE);
    }

    @GetMapping("/menu")
    public ParentContentView.StandingDoc menu(
            @RequestHeader("authorization") String authHeader) throws Exception {
        return parentContentService.standingDoc(studentOf(authHeader),
                mthiebi.sgs.gradebook.model.PostKind.MENU);
    }

    @GetMapping("/characterizations")
    public List<ParentContentView.Characterization> characterizations(
            @RequestHeader("authorization") String authHeader) throws Exception {
        return parentContentService.characterizations(studentOf(authHeader));
    }

    /**
     * Which boxes this child's school shows.
     * <p>
     * The console renders from this rather than deciding for itself, so the
     * primary/basic/secondary rule lives in one place - next to the school code
     * it is derived from.
     */
    /**
     * A month of the child's descriptions, in the homework calendar's shape.
     */
    @GetMapping("/characterizations/month")
    public ParentContentView.HomeworkMonth descriptionMonth(
            @RequestHeader("authorization") String authHeader,
            @RequestParam String month) throws Exception {
        return parentContentService.descriptionMonth(studentOf(authHeader), month);
    }

    /**
     * One day's descriptions, grouped by subject.
     */
    @GetMapping("/characterizations/day/{date}")
    public ParentContentView.HomeworkDayDetail descriptionDay(
            @RequestHeader("authorization") String authHeader,
            @PathVariable String date) throws Exception {
        return parentContentService.descriptionDay(studentOf(authHeader), date);
    }

    @GetMapping("/modules")
    public List<String> modules(@RequestHeader("authorization") String authHeader)
            throws Exception {
        return parentContentService.modules(studentOf(authHeader));
    }

    // ---- news ---------------------------------------------------------------

    /**
     * Published news, newest first.
     * <p>
     * No student scoping: news is institution-wide by the school's decision, and
     * the category is a label to filter on rather than a visibility rule. The
     * token is still required - this is behind the parent portal, not open.
     */
    @GetMapping("/news")
    public ParentContentView.NewsPage news(
            @RequestHeader("authorization") String authHeader,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws Exception {
        studentOf(authHeader);
        return parentContentService.news(categoryId, page, size);
    }

    /**
     * The categories news is filed under, for the filter on the news page.
     * <p>
     * Names and ids only. A category is a label the school invented; there is
     * nothing about it a parent should not see, and the alternative is a filter
     * whose options the console has to hardcode.
     */
    /**
     * One news item, for an article opened by its own address.
     * <p>
     * Declared after the literal routes above it, which Spring prefers anyway -
     * /news/categories is a path, not a uuid.
     */
    @GetMapping("/news/{uuid}")
    public ParentContentView.NewsItem newsItem(
            @RequestHeader("authorization") String authHeader,
            @PathVariable String uuid) throws Exception {
        studentOf(authHeader);
        return parentContentService.newsItem(uuid);
    }

    @GetMapping("/news/categories")
    public List<NewsCategory> newsCategories(
            @RequestHeader("authorization") String authHeader) throws Exception {
        studentOf(authHeader);
        return categoryService.list().stream()
                .map(c -> new NewsCategory(c.getId(), c.getName()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * A news picture.
     * <p>
     * Its own route rather than reusing the staff one, which is MANAGE_NEWS and
     * would 403 for every parent. Behind the parent token like everything else
     * here, so the pictures are not simply open on the internet.
     * <p>
     * The content type is what we wrote, not what was uploaded - the bytes were
     * re-encoded from decoded pixels, so it is the truth about them rather than
     * a claim the uploader made. Cached hard: the uuid changes when the picture
     * does.
     */
    @GetMapping("/news/images/{uuid}")
    public org.springframework.http.ResponseEntity<byte[]> newsImage(
            @RequestHeader("authorization") String authHeader,
            @PathVariable String uuid) throws Exception {
        studentOf(authHeader);
        mthiebi.sgs.gradebook.model.PostImage image = imageService.get(uuid);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType
                        .parseMediaType(image.getContentType()))
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL,
                        "private, max-age=31536000, immutable")
                .body(image.getBytes());
    }

    /**
     * A filter option.
     */
    public static class NewsCategory {
        private final Long id;
        private final String name;

        public NewsCategory(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * One child, one day, one batch of assignments they have now read.
     */
    public static class SeenRequest {
        private List<String> postUuids;

        public List<String> getPostUuids() {
            return postUuids;
        }

        public void setPostUuids(List<String> postUuids) {
            this.postUuids = postUuids;
        }
    }

    /**
     * One journal, as this student's parent sees it.
     * <p>
     * The shape follows the journal: a per-subject journal offers a period and
     * lists subjects; a class-wide one lists its periods. Passing a subject
     * narrows to one row, which is what the console draws as cards.
     */
    @GetMapping("/journals/{uuid}")
    public ParentView view(@RequestHeader("authorization") String authHeader,
                           @PathVariable String uuid,
                           @RequestParam(required = false) Long periodId,
                           @RequestParam(required = false) Long subjectId,
                           // Which tier the landing page's button opens on.
                           // Only consulted when periodId is absent - once the
                           // parent uses the picker, their choice is the answer.
                           @RequestParam(required = false) PeriodKind periodKind,
                           // Every subject, so one mark each rather than every
                           // column: five subjects by twelve columns is a grid
                           // nobody asked for.
                           @RequestParam(defaultValue = "false") boolean summary)
            throws Exception {
        return parentViewService.view(studentOf(authHeader), uuid, periodId, subjectId,
                periodKind, summary);
    }

    /**
     * Never a request parameter. A student id in the query string would let any
     * logged-in parent read any child's grades by changing a number.
     */
    private Long studentOf(String authHeader) throws Exception {
        return Long.valueOf(utilsJwt.getUsernameFromHeader(authHeader));
    }
}
