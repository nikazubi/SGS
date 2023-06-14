package mthiebi.sgs.service;

import mthiebi.sgs.models.ChangeRequest;
import mthiebi.sgs.models.ChangeRequestStatus;

import java.util.List;

public interface ChangeRequestService {

    ChangeRequest createChangeRequest(ChangeRequest changeRequest, String username);

    void changeRequestStatus(Long changeRequestId, ChangeRequestStatus changeRequestStatus);

    List<ChangeRequest> getChangeRequests(String username);

}
