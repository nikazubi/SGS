package mthiebi.sgs.impl;

import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.Grade;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.Subject;
import mthiebi.sgs.repository.AcademyClassRepository;
import mthiebi.sgs.repository.GradeRepository;
import mthiebi.sgs.repository.StudentRepository;
import mthiebi.sgs.repository.SubjectRepository;
import mthiebi.sgs.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GradeServiceImpl implements GradeService {

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AcademyClassRepository academyClassRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Override
    public Grade insertStudentGrade(Grade grade) {
        Student student = studentRepository.findById(grade.getStudent().getId()).orElseThrow();
        AcademyClass academyClass = academyClassRepository.findById(grade.getAcademyClass().getId()).orElseThrow();
        Subject subject = subjectRepository.findById(grade.getSubject().getId()).orElseThrow();
        grade.setStudent(student);
        grade.setAcademyClass(academyClass);
        grade.setSubject(subject);
        return gradeRepository.save(grade);
    }

    @Override
    public Grade updateStudentGrade(Grade grade) {
        Grade oldGrade = gradeRepository.findById(grade.getId()).orElseThrow();
        oldGrade.setValue(grade.getValue());
        return gradeRepository.save(oldGrade);
    }

    @Override
    public void deleteGrade(Long id) {
        gradeRepository.deleteById(id);
    }

    @Override
    public List<Grade> getStudentGradeByStudentId(Long studentId) {
        return gradeRepository.findGradeByStudentId(studentId);
    }

    @Override
    public List<Grade> getStudentGradeByClassId(Long classId) {
        return gradeRepository.findGradeByAcademyClassId(classId);
    }

    @Override
    public List<Grade> getStudentGradeByClassAndSubjectId(Long classId, Long studentId) {
        return gradeRepository.findGradeByAcademyClassIdAndStudentId(classId, studentId);
    }
}
