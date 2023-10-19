package mthiebi.sgs.service;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.Subject;

import java.util.List;

public interface SubjectService {

    Subject createSubject(Subject subject);

    Subject updateSubject(Subject subject) throws SGSException;

    void deleteSubject(Long id);

    List<Subject> getSubjects(int limit,
                              int page,
                              Long id,
                              String name,
                              String userName) throws SGSException;

    List<Subject> getSubjectsForStudent(String username) throws SGSException;

    List<Subject> getSubjects(int limit,
                              int page,
                              Long id,
                              String name) throws SGSException;

    Subject findSubjectById(Long id) throws SGSException;

}
