package mthiebi.sgs.repository;

import mthiebi.sgs.models.Subject;

import java.util.List;

public interface SubjectRepositoryCustom {

    List<Subject> findByIds(List<Long> ids);
}
