package mthiebi.sgs.impl;

import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.Subject;
import mthiebi.sgs.repository.AcademyClassRepository;
import mthiebi.sgs.repository.StudentRepository;
import mthiebi.sgs.repository.SubjectRepository;
import mthiebi.sgs.service.AcademyClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import mthiebi.sgs.models.AcademyClass;

import java.util.ArrayList;
import java.util.List;

@Service
public class AcademyClassServiceImpl implements AcademyClassService {

    @Autowired
    private AcademyClassRepository academyClassRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Override
    public AcademyClass createAcademyClass(AcademyClass academyClass) {
        return academyClassRepository.save(academyClass);
        // todo validations - exceptions
    }

    @Override
    public AcademyClass updateAcademyClass(AcademyClass academyClass) {
        AcademyClass oldAcademyClass = academyClassRepository.findById(academyClass.getId()).orElseThrow();
        oldAcademyClass.setClassLevel(academyClass.getClassLevel());
        oldAcademyClass.setClassName(academyClass.getClassName());
        oldAcademyClass.setStudentList(academyClass.getStudentList());
        academyClassRepository.save(oldAcademyClass);
        return oldAcademyClass;
    }

    @Override
    public void deleteAcademyClass(Long id) {
        academyClassRepository.deleteById(id);
    }

    @Override
    public List<AcademyClass> getAcademyClasses() {
        return academyClassRepository.findAll();
    }

    @Override
    public List<AcademyClass> getAcademyClasses(String queryKey) {
        return academyClassRepository.getAcademyClasses(queryKey);
    }

    @Override
    public AcademyClass findAcademyClassById(Long id) {
        return academyClassRepository.findById(id).orElseThrow();
    }

    @Override
    public void attachStudentsToAcademyClass(Long academyClassId, List<Long> studentIdList) {
        AcademyClass academyClass = academyClassRepository.findById(academyClassId).orElseThrow();

        List<Student> studentList = new ArrayList<>();
        for (Long studentId : studentIdList) {
            studentRepository.findById(studentId).ifPresent(studentList::add);
        }
        academyClass.setStudentList(studentList);
        academyClassRepository.save(academyClass);
    }

    @Override
    public void attachSubjectsToAcademyClass(Long academyClassId, List<Long> subjectIdList) {
        AcademyClass academyClass = academyClassRepository.findById(academyClassId).orElseThrow();

        List<Subject> subjectList = new ArrayList<>();
        for (Long subjectId : subjectIdList) {
            subjectRepository.findById(subjectId).ifPresent(subjectList::add);
        }
        academyClass.setSubjectList(subjectList);
        academyClassRepository.save(academyClass);
    }
}
