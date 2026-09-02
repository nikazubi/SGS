package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.gradebook.model.PostKind;
import mthiebi.sgs.gradebook.service.content.PostDraft;
import mthiebi.sgs.gradebook.service.content.PostService;
import mthiebi.sgs.gradebook.service.content.PostView;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Homework.
 * <p>
 * Every endpoint is scoped by {@link ClassScopeGuard}. A permission says what
 * someone may do and has never said where; dropping the scope check is how the
 * first review found that any teacher who could enter grades could enter them
 * for the whole school.
 * <p>
 * The school confirmed class is the right axis: opening homework shows all of
 * the class's subjects, so there is no subject-level narrowing.
 */
@RestController
@RequestMapping("/api/gradebook/homework")
public class HomeworkController {

    @Autowired
    private PostService postService;

    @Autowired
    private ClassScopeGuard classScope;

    @Autowired
    private ActorResolver actorResolver;

    /**
     * The list behind one subject's accordion.
     * <p>
     * {@code limit} is the "top few" the accordion shows; omitting it is the
     * "see more" dialog asking for the lot.
     */
    @GetMapping
    @Secured({AuthConstants.MANAGE_HOMEWORK})
    public List<PostView> list(@RequestParam Long classGroupId,
                               @RequestParam(required = false) Long subjectId,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(required = false) Integer limit,
                               @RequestHeader("authorization") String authHeader)
            throws SGSException {
        classScope.check(authHeader, classGroupId);
        return postService.list(PostKind.HOMEWORK, classGroupId, subjectId, from, to, limit);
    }

    /**
     * How many exist, so the list knows whether "see more" has anything behind it.
     */
    @GetMapping("/count")
    @Secured({AuthConstants.MANAGE_HOMEWORK})
    public long count(@RequestParam Long classGroupId,
                      @RequestParam(required = false) Long subjectId,
                      @RequestParam(required = false)
                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                      @RequestParam(required = false)
                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                      @RequestHeader("authorization") String authHeader) throws SGSException {
        classScope.check(authHeader, classGroupId);
        return postService.count(PostKind.HOMEWORK, classGroupId, subjectId, from, to);
    }

    @GetMapping("/{uuid}")
    @Secured({AuthConstants.MANAGE_HOMEWORK})
    public PostView get(@PathVariable String uuid,
                        @RequestHeader("authorization") String authHeader) throws SGSException {
        return require(uuid, authHeader);
    }

    /**
     * The post, checked against this endpoint rather than trusted because a
     * uuid was supplied.
     * <p>
     * Two checks, both necessary. The class comes from the row rather than from
     * a parameter the caller chose, or the uuid alone would read another class's
     * homework. And the kind is verified, or MANAGE_HOMEWORK would publish a
     * news item by putting its uuid in a homework URL - the permission is per
     * module, so the check has to be too.
     */
    private PostView require(String uuid, String authHeader) throws SGSException {
        PostView view = postService.get(uuid);
        classScope.check(authHeader, view.getClassGroupId());
        if (!PostKind.HOMEWORK.name().equals(view.getKind())) {
            throw new mthiebi.sgs.SGSException(mthiebi.sgs.SGSExceptionCode.BAD_REQUEST,
                    "ჩანაწერი სხვა ტიპისაა");
        }
        return view;
    }

    @PostMapping
    @Secured({AuthConstants.MANAGE_HOMEWORK})
    public PostView save(@RequestBody PostDraft draft,
                         @RequestHeader("authorization") String authHeader) throws SGSException {
        classScope.check(authHeader, draft.getClassGroupId());
        if (draft.getUuid() != null && !draft.getUuid().isEmpty()) {
            // An existing post could otherwise be moved into the caller's own
            // class by a draft that names one they do hold.
            require(draft.getUuid(), authHeader);
        }
        return postService.save(PostKind.HOMEWORK, draft, actorResolver.idOf(authHeader));
    }

    /**
     * Release it to parents. No approval: this is not the grade publish flow.
     */
    @PostMapping("/{uuid}/publish")
    @Secured({AuthConstants.MANAGE_HOMEWORK})
    public PostView publish(@PathVariable String uuid,
                            @RequestHeader("authorization") String authHeader)
            throws SGSException {
        require(uuid, authHeader);
        return postService.publish(uuid, actorResolver.idOf(authHeader));
    }

    /**
     * Soft delete, so something a parent has read leaves a trace.
     */
    @PostMapping("/{uuid}/archive")
    @Secured({AuthConstants.MANAGE_HOMEWORK})
    public void archive(@PathVariable String uuid,
                        @RequestParam(defaultValue = "true") boolean archived,
                        @RequestHeader("authorization") String authHeader) throws SGSException {
        require(uuid, authHeader);
        postService.archive(uuid, archived, actorResolver.idOf(authHeader));
    }
}
