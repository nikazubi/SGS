package mthiebi.sgs.service;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.ChangeRequest;
import mthiebi.sgs.models.ChangeRequestStatus;

import java.util.Date;
import java.util.List;

public interface ChangeRequestService {

    ChangeRequest createChangeRequest(ChangeRequest changeRequest, String username) throws SGSException;

    void changeRequestStatus(Long changeRequestId, ChangeRequestStatus changeRequestStatus, String description) throws SGSException;

    List<ChangeRequest> getChangeRequests(String username, Long classId, Long studentId, Date date) throws SGSException;

    Date getLastUpdateTime();
}
