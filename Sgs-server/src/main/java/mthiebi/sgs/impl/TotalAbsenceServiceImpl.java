package mthiebi.sgs.impl;

import mthiebi.sgs.ExceptionKeys;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.TotalAbsence;
import mthiebi.sgs.repository.AcademyClassRepository;
import mthiebi.sgs.repository.StudentRepository;
import mthiebi.sgs.repository.TotalAbsenceRepository;
import mthiebi.sgs.service.TotalAbsenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TotalAbsenceServiceImpl implements TotalAbsenceService {

    @Autowired
    private AcademyClassRepository academyClassRepository;

    @Autowired
    private TotalAbsenceRepository totalAbsenceRepository;

    @Autowired
    private StudentRepository studentRepository;


    @Override
    public List<TotalAbsence> filter(Long academyClass, Date activePeriod) {
        return totalAbsenceRepository.filter(academyClass, activePeriod);
    }

    @Override
    public List<TotalAbsence> filter(String username, Date activePeriod) {
        Student student = studentRepository.findByUsername(username).orElseThrow();
        AcademyClass academyClass = academyClassRepository.getAcademyClassByStudent(student.getId()).orElseThrow();
        return totalAbsenceRepository.filter(academyClass.getId(), activePeriod);
    }

    @Override
    public List<TotalAbsence> getCurrentAbsencesForEveryClass() {
        List<AcademyClass> allAcademyClasses = academyClassRepository.findAll();
        return allAcademyClasses.stream().map(AcademyClass::getActiveTotalAbsence).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addTotalAbsenceToAcademyClass(long academyClassId, TotalAbsence totalAbsence) throws SGSException {
        AcademyClass academyClass = academyClassRepository.findById(academyClassId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.ACADEMY_CLASS_NOT_FOUND));
        List<TotalAbsence> existingTotalOfYearAndMonth = academyClass.getTotalAbsences().stream().filter(t -> {
            Calendar existingActivePeriod = Calendar.getInstance();
            existingActivePeriod.setTime(t.getActivePeriod());
            Calendar newActivePeriod = Calendar.getInstance();
            newActivePeriod.setTime(totalAbsence.getActivePeriod());
            return existingActivePeriod.get(Calendar.YEAR) == newActivePeriod.get(Calendar.YEAR) &&
                    existingActivePeriod.get(Calendar.MONTH) == newActivePeriod.get(Calendar.MONTH); })
                .collect(Collectors.toList());
        if(!existingTotalOfYearAndMonth.isEmpty()){
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.TOTAL_ACADEMIC_HOUR_OF_CLASS_AND_DATE_ALREADY_EXISTS);
        }
        totalAbsence.setAcademyClass(academyClass);
        TotalAbsence savedTotalAbsence = totalAbsenceRepository.save(totalAbsence);
        academyClass.addTotalAbsence(savedTotalAbsence);
    }

    @Override
    public void addTotalAbsenceToAcademyClasses(List<AcademyClass> academyClasses, Long totalAcademicHour, Date activePeriod) throws SGSException {
        for (AcademyClass academyClass: academyClasses) {
            TotalAbsence totalAbsence = TotalAbsence.builder()
                    .totalAcademyHour(totalAcademicHour)
                    .activePeriod(activePeriod)
                    .build();
            addTotalAbsenceToAcademyClass(academyClass.getId(), totalAbsence);
        }
    }

    @Override
    @Transactional
    public void deleteById(long id) throws SGSException {
        TotalAbsence totalAbsence = totalAbsenceRepository.findById(id)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.TOTAL_ACADEMIC_HOUR_NOT_FOUND));
        AcademyClass academyClass = totalAbsence.getAcademyClass();
        totalAbsence.setAcademyClass(null);
        academyClass.getTotalAbsences().removeIf(totalAbsence1 -> totalAbsence1.getId().equals(totalAbsence.getId()));
        totalAbsenceRepository.deleteById(id);
    }
}
