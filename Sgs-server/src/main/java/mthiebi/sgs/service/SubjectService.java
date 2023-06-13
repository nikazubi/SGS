package mthiebi.sgs.service;

import mthiebi.sgs.models.Subject;

import javax.persistence.EntityManager;
import java.util.List;

public interface SubjectService {

    Subject createSubject(Subject subject);

    Subject updateSubject(Subject subject);

    void deleteSubject(Long id);

    List<Subject> getSubjects(int limit,
                              int page,
                              Long id,
                              String name,
                              String userName);

    Subject findSubjectById(Long id);

}
