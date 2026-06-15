package mthiebi.sgs.service;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.AcademyClass;

import java.util.List;

public interface AcademyClassService {

    AcademyClass createAcademyClass(AcademyClass academyClass, String username) throws SGSException;

    AcademyClass updateAcademyClass(AcademyClass academyClass) throws SGSException;

    void deleteAcademyClass(Long id);

    List<AcademyClass> getAcademyClasses(String queryKey);

    List<AcademyClass> getAcademyClasses(String username, String queryKey) throws SGSException;

    AcademyClass findAcademyClassById(Long id);

    Boolean isStudentInTransitClass(Long studentId);

    void attachStudentsToAcademyClass(Long academyClassId, List<Long> studentIdList) throws SGSException;

    void attachSubjectsToAcademyClass(Long academyClassId, List<Long> subjectIdList) throws SGSException;
}
