package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.gradebook.model.PostKind;
import mthiebi.sgs.gradebook.service.content.PostDraft;
import mthiebi.sgs.gradebook.service.content.PostService;
import mthiebi.sgs.gradebook.service.content.PostView;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The daily schedule and the meal menu.
 * <p>
 * The same document with a different number of columns per row. The school
 * confirmed they are entered once for the year and adjusted occasionally - no
 * months, no trimesters, no weekly versions - so there is exactly one of each
 * per class and no list to page through.
 * <p>
 * **Deliberately six endpoints rather than three taking a kind parameter.**
 * `@Secured({MANAGE_SCHEDULE, MANAGE_MENU})` is an *or*: one shared endpoint
 * would let anyone holding either permission edit both documents, which is
 * precisely what having two permissions is meant to prevent. The duplication is
 * three thin methods; the alternative is a silent authorization hole.
 */
@RestController
@RequestMapping("/api/gradebook")
public class StandingDocController {

    @Autowired
    private PostService postService;

    @Autowired
    private ClassScopeGuard classScope;

    @Autowired
    private ActorResolver actorResolver;

    // ---- schedule ---------------------------------------------------------

    /**
     * Null when the class has not made one yet, which the console reads as "new".
     */
    @GetMapping("/schedule")
    @Secured({AuthConstants.MANAGE_SCHEDULE})
    public PostView getSchedule(@RequestParam Long classGroupId,
                                @RequestHeader("authorization") String authHeader)
            throws SGSException {
        return read(PostKind.SCHEDULE, classGroupId, authHeader);
    }

    @PostMapping("/schedule")
    @Secured({AuthConstants.MANAGE_SCHEDULE})
    public PostView saveSchedule(@RequestBody PostDraft draft,
                                 @RequestHeader("authorization") String authHeader)
            throws SGSException {
        return write(PostKind.SCHEDULE, draft, authHeader);
    }

    @PostMapping("/schedule/{uuid}/publish")
    @Secured({AuthConstants.MANAGE_SCHEDULE})
    public PostView publishSchedule(@PathVariable String uuid,
                                    @RequestHeader("authorization") String authHeader)
            throws SGSException {
        return release(uuid, PostKind.SCHEDULE, authHeader);
    }

    // ---- menu -------------------------------------------------------------

    @GetMapping("/menu")
    @Secured({AuthConstants.MANAGE_MENU})
    public PostView getMenu(@RequestParam Long classGroupId,
                            @RequestHeader("authorization") String authHeader)
            throws SGSException {
        return read(PostKind.MENU, classGroupId, authHeader);
    }

    @PostMapping("/menu")
    @Secured({AuthConstants.MANAGE_MENU})
    public PostView saveMenu(@RequestBody PostDraft draft,
                             @RequestHeader("authorization") String authHeader)
            throws SGSException {
        return write(PostKind.MENU, draft, authHeader);
    }

    @PostMapping("/menu/{uuid}/publish")
    @Secured({AuthConstants.MANAGE_MENU})
    public PostView publishMenu(@PathVariable String uuid,
                                @RequestHeader("authorization") String authHeader)
            throws SGSException {
        return release(uuid, PostKind.MENU, authHeader);
    }

    // ---- shared -----------------------------------------------------------

    private PostView read(PostKind kind, Long classGroupId, String authHeader)
            throws SGSException {
        classScope.check(authHeader, classGroupId);
        return postService.standing(kind, classGroupId);
    }

    private PostView write(PostKind kind, PostDraft draft, String authHeader)
            throws SGSException {
        classScope.check(authHeader, draft.getClassGroupId());
        if (draft.getUuid() != null && !draft.getUuid().isEmpty()) {
            PostView existing = postService.get(draft.getUuid());
            // Otherwise another class's document could be pulled into this one
            // by a draft naming a class the caller does hold.
            classScope.check(authHeader, existing.getClassGroupId());
            requireKind(existing, kind);
        }
        return postService.save(kind, draft, actorResolver.idOf(authHeader));
    }

    private PostView release(String uuid, PostKind kind, String authHeader)
            throws SGSException {
        PostView existing = postService.get(uuid);
        classScope.check(authHeader, existing.getClassGroupId());
        requireKind(existing, kind);
        return postService.publish(uuid, actorResolver.idOf(authHeader));
    }

    /**
     * The endpoint's permission only means anything if the post is of the kind
     * that endpoint governs. Without this, MANAGE_MENU publishes a schedule by
     * putting its uuid in the menu URL.
     */
    private void requireKind(PostView view, PostKind expected) throws SGSException {
        if (!expected.name().equals(view.getKind())) {
            throw new SGSException(mthiebi.sgs.SGSExceptionCode.BAD_REQUEST,
                    "ჩანაწერი სხვა ტიპისაა");
        }
    }
}
