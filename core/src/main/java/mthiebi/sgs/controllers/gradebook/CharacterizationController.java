package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
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
 * Written accounts of a student, per subject.
 * <p>
 * The same shape as homework with fewer fields: a date, exactly one student and
 * a body. No title - neither the brief's form nor the school's description has
 * one.
 */
@RestController
@RequestMapping("/api/gradebook/characterization")
public class CharacterizationController {

    @Autowired
    private PostService postService;

    @Autowired
    private ClassScopeGuard classScope;

    @Autowired
    private ActorResolver actorResolver;

    @GetMapping
    @Secured({AuthConstants.MANAGE_CHARACTERIZATION})
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
        return postService.list(PostKind.CHARACTERIZATION, classGroupId, subjectId,
                from, to, limit);
    }

    @GetMapping("/{uuid}")
    @Secured({AuthConstants.MANAGE_CHARACTERIZATION})
    public PostView get(@PathVariable String uuid,
                        @RequestHeader("authorization") String authHeader) throws SGSException {
        return require(uuid, authHeader);
    }

    @PostMapping
    @Secured({AuthConstants.MANAGE_CHARACTERIZATION})
    public PostView save(@RequestBody PostDraft draft,
                         @RequestHeader("authorization") String authHeader) throws SGSException {
        classScope.check(authHeader, draft.getClassGroupId());
        if (draft.getUuid() != null && !draft.getUuid().isEmpty()) {
            require(draft.getUuid(), authHeader);
        }
        // Exactly one student, unlike homework's "some or all". A
        // characterization with no subject is meaningless too - the page opens
        // on the list of subjects.
        if (draft.getTargetEnrollmentIds().size() != 1) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "დახასიათება ერთ მოსწავლეს ეხება");
        }
        if (draft.getSubjectId() == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "საგანი აუცილებელია");
        }
        return postService.save(PostKind.CHARACTERIZATION, draft,
                actorResolver.idOf(authHeader));
    }

    @PostMapping("/{uuid}/publish")
    @Secured({AuthConstants.MANAGE_CHARACTERIZATION})
    public PostView publish(@PathVariable String uuid,
                            @RequestHeader("authorization") String authHeader)
            throws SGSException {
        require(uuid, authHeader);
        return postService.publish(uuid, actorResolver.idOf(authHeader));
    }

    @PostMapping("/{uuid}/archive")
    @Secured({AuthConstants.MANAGE_CHARACTERIZATION})
    public void archive(@PathVariable String uuid,
                        @RequestParam(defaultValue = "true") boolean archived,
                        @RequestHeader("authorization") String authHeader) throws SGSException {
        require(uuid, authHeader);
        postService.archive(uuid, archived, actorResolver.idOf(authHeader));
    }

    /**
     * Class from the row, and the kind verified - see HomeworkController.require.
     */
    private PostView require(String uuid, String authHeader) throws SGSException {
        PostView view = postService.get(uuid);
        classScope.check(authHeader, view.getClassGroupId());
        if (!PostKind.CHARACTERIZATION.name().equals(view.getKind())) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "ჩანაწერი სხვა ტიპისაა");
        }
        return view;
    }
}
