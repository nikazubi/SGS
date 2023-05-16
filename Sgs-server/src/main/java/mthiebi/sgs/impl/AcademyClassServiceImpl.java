package mthiebi.sgs.impl;

import mthiebi.sgs.models.Student;
import mthiebi.sgs.repository.AcademyClassRepository;
import mthiebi.sgs.repository.StudentRepository;
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

    @Override
    public AcademyClass createAcademyClass(AcademyClass academyClass) {
        return academyClassRepository.save(academyClass);
        // todo validations - exceptions
    }

    @Override
    public AcademyClass updateAcademyClass(AcademyClass academyClass) {
        AcademyClass oldAcademyClass = academyClassRepository.findById(academyClass.getId()).orElseThrow();
        oldAcademyClass.setClassLevel(academyClass.getClassLevel());
        oldAcademyClass.setClassLevelIndex(academyClass.getClassLevelIndex());
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
    public void attachStudentsToAcademyClass(Long academyClassId, List<Long> studentIdList) {
        AcademyClass academyClass = academyClassRepository.findById(academyClassId).orElseThrow();

        List<Student> studentList = new ArrayList<>();
        for (Long studentId : studentIdList) {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student != null) {
                studentList.add(student);
            }
        }
        academyClass.setStudentList(studentList);
        academyClassRepository.save(academyClass);
    }
}
