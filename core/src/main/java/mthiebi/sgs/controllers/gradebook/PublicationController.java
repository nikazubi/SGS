package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.gradebook.model.ChangeRequestStatus;
import mthiebi.sgs.gradebook.service.publish.ChangeRequestService;
import mthiebi.sgs.gradebook.service.publish.ChangeRequestView;
import mthiebi.sgs.gradebook.service.publish.DecideChangeRequest;
import mthiebi.sgs.gradebook.service.publish.PublicationLogEntry;
import mthiebi.sgs.gradebook.service.publish.PublicationResult;
import mthiebi.sgs.gradebook.service.publish.PublicationService;
import mthiebi.sgs.gradebook.service.publish.PublishRequest;
import mthiebi.sgs.gradebook.service.publish.RaiseChangeRequest;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Releasing grades to parents, and changing them afterwards.
 * <p>
 * Publishing turns the teachers' working journal into what parents read, and
 * locks every released cell. From then on a correction is a request the teacher
 * explains and the director signs off.
 */
@RestController
@RequestMapping("/api/gradebook")
public class PublicationController {

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private ChangeRequestService changeRequestService;

    @Autowired
    private ActorResolver actorResolver;

    @Autowired
    private ClassScopeGuard classScope;

    // ---- publish --------------------------------------------------------

    /**
     * Release a (class, period), optionally narrowed to one subject.
     * <p>
     * Republishing is normal rather than an error: it picks up marks entered
     * since and values whose inputs have moved.
     */
    @PostMapping("/publish")
    @Secured({AuthConstants.MANAGE_CLOSED_PERIOD})
    public PublicationResult publish(@RequestHeader("authorization") String authHeader,
                                     @RequestBody PublishRequest request) throws SGSException {
        // Publishing is outward-facing, so scope matters more here than
        // anywhere: without it one teacher could release the whole school.
        classScope.check(authHeader, request.getClassGroupId());
        return publicationService.publish(request.getClassGroupId(), request.getPeriodId(),
                request.getSubjectId(), request.getJournalUuid(),
                actorResolver.idOf(authHeader));
    }

    /**
     * The release log — who published what, and when.
     * <p>
     * Scoped. The console asks for every class, so an unscoped read handed a
     * class-restricted user the whole school's release history.
     */
    @GetMapping("/publications")
    @Secured({AuthConstants.MANAGE_CLOSED_PERIOD, AuthConstants.MANAGE_GRADES})
    public List<PublicationLogEntry> publications(
            @RequestHeader("authorization") String authHeader,
            @RequestParam(required = false) Long classGroupId) throws SGSException {
        classScope.check(authHeader, classGroupId);
        return publicationService.log(classGroupId,
                classScope.visibleClassGroupIds(authHeader));
    }

    // ---- change requests ------------------------------------------------

    /**
     * Raise one.
     * <p>
     * ADD_GRADES, not MANAGE_CHANGE_REQUESTS: a teacher has to be able to ask.
     * The legacy endpoint required the approval permission to create a request,
     * so the only people who could raise one were the people who approve them.
     */
    @PostMapping("/change-requests")
    @Secured({AuthConstants.ADD_GRADES})
    public ChangeRequestView raise(@RequestHeader("authorization") String authHeader,
                                   @RequestBody RaiseChangeRequest request) throws SGSException {
        classScope.checkCell(authHeader, request.getGradeEntryId());
        return changeRequestService.raise(request, actorResolver.idOf(authHeader));
    }

    /**
     * The queue, scoped.
     * <p>
     * The console sends no class, so this used to return every class's requests
     * to anyone holding VIEW_CHANGE_REQUESTS - student names, both values, and
     * the teacher's stated reason.
     */
    @GetMapping("/change-requests")
    @Secured({AuthConstants.VIEW_CHANGE_REQUESTS, AuthConstants.MANAGE_CHANGE_REQUESTS})
    public List<ChangeRequestView> queue(@RequestHeader("authorization") String authHeader,
                                         @RequestParam(required = false) ChangeRequestStatus status,
                                         @RequestParam(required = false) Long classGroupId)
            throws SGSException {
        classScope.check(authHeader, classGroupId);
        return changeRequestService.queue(status, classGroupId,
                classScope.visibleClassGroupIds(authHeader));
    }

    /**
     * Approve or reject.
     * <p>
     * Approving writes the value and releases it together with every published
     * cell it moved — otherwise parents would see the corrected mark beside an
     * average still computed from the old one.
     */
    @PostMapping("/change-requests/decide")
    @Secured({AuthConstants.MANAGE_CHANGE_REQUESTS})
    public ChangeRequestView decide(@RequestHeader("authorization") String authHeader,
                                    @RequestBody DecideChangeRequest request) throws SGSException {
        // Scoped on the request's own class, not the caller's word for it. This
        // is the most consequential unguarded endpoint there was: approving
        // writes the value through the privileged path that deliberately lifts
        // the publication lock, releases it, and emails that child's guardian -
        // so a coordinator scoped to one class could alter and publish a mark
        // for a child in another.
        classScope.check(authHeader,
                changeRequestService.classGroupOf(request.getChangeRequestId()));
        return changeRequestService.decide(request.getChangeRequestId(), request.isApprove(),
                request.getComment(), actorResolver.idOf(authHeader));
    }
}
