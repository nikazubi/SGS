package mthiebi.sgs.impl;

import mthiebi.sgs.models.Subject;
import mthiebi.sgs.repository.SubjectRepository;
import mthiebi.sgs.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Service
public class SubjectServiceImpl implements SubjectService {

    @Autowired
    private SubjectRepository subjectRepository;

    @PersistenceContext
    private EntityManager em;

    @Override
    public Subject createSubject(Subject subject) {
        return subjectRepository.save(subject);
    }

    @Override
    public Subject updateSubject(Subject subject) {
        Subject oldSubject = subjectRepository.findById(subject.getId()).orElseThrow();
        oldSubject.setName(subject.getName());
        return subjectRepository.save(oldSubject);
    }

    @Override
    public void deleteSubject(Long id) {
        subjectRepository.deleteById(id);
    }

    @Override
    public List<Subject> getSubjects(int limit,
                                     int page,
                                     Long id,
                                     String name) {
        return subjectRepository.findAllSubject(limit, page, id, name, em);
    }

    @Override
    public Subject findSubjectById(Long id) {
        return subjectRepository.findById(id).orElseThrow();
    }

}
