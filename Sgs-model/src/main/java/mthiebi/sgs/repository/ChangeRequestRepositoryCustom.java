package mthiebi.sgs.repository;

import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.ChangeRequest;
import mthiebi.sgs.models.QChangeRequest;

import java.util.Date;
import java.util.List;

public interface ChangeRequestRepositoryCustom {

    List<ChangeRequest> getChangeRequests(List<AcademyClass> academyClassList, Long classId, Long StudentId, Date date);

    Date getLastUpdateDate();
}
