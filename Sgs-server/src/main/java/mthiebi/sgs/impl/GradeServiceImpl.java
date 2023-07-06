package mthiebi.sgs.impl;

import mthiebi.sgs.models.*;
import mthiebi.sgs.repository.AcademyClassRepository;
import mthiebi.sgs.repository.GradeRepository;
import mthiebi.sgs.repository.StudentRepository;
import mthiebi.sgs.repository.SubjectRepository;
import mthiebi.sgs.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
        AcademyClass academyClass = academyClassRepository.getAcademyClassByStudent(student.getId()).orElseThrow();
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
        return gradeRepository.findGradeByAcademyClassIdAndSubjectId(classId, studentId);
    }

    @Override
    public List<Grade> getStudentGradeByClassAndSubjectIdAndCreateTime(Long classId,
                                                                       Long subjectId,
                                                                       Long studentId,
                                                                       Date createTime,
                                                                       String gradeTypePrefix) {
        List<Student> allStudentsInAcademyClass = studentRepository.findAllByAcademyClass(classId);
        Subject currSubject = subjectRepository.findById(subjectId).orElse(null);
        AcademyClass academyClass = academyClassRepository.findById(classId).orElse(null);

        List<Grade> existingGrades =  gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndCreateTime(classId, subjectId, studentId, createTime)
                                                        .stream()
                                                        .filter(grade -> grade.getGradeType().toString().startsWith(gradeTypePrefix))
                                                        .collect(Collectors.toList());
        return fillWithEmptyGradeListOfGradeType(allStudentsInAcademyClass, gradeTypePrefix, academyClass, currSubject, existingGrades);
    }

    private List<Grade> fillWithEmptyGradeListOfGradeType(List<Student> students,
                                                          String gradeTypePrefix,
                                                          AcademyClass academyClass,
                                                          Subject subject,
                                                          List<Grade> existingGrades){
       List<GradeType> gradeTypes = Arrays.stream(GradeType.values())
                                                    .filter(gradeType -> gradeType.toString().startsWith(gradeTypePrefix))
                                                    .collect(Collectors.toList());
        List<Grade> result = new ArrayList<>(existingGrades);
        for (Student student : students) {
            for(GradeType gradeType : gradeTypes){
                if (existingGrades.stream().filter(grade -> grade.getGradeType() == gradeType && student.getId() == grade.getStudent().getId()).findAny().isEmpty()){
                    Grade grade = buildGradeOfGradeType(gradeType, academyClass, student, subject);
                    result.add(grade);
                }
            }
        }
        return result;
    }

    private Grade buildGradeOfGradeType(GradeType gradeType, AcademyClass academyClass, Student student, Subject subject){
        return  Grade.builder()
                    .gradeType(gradeType)
                    .academyClass(academyClass)
                    .student(student)
                    .subject(subject)
                    .value(null)
                    .build();
    }
}
