package mthiebi.sgs.service;

import java.util.List;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.AcademyClass;

public interface AcademyClassService {

    AcademyClass createAcademyClass(AcademyClass academyClass);

    AcademyClass updateAcademyClass(AcademyClass academyClass) throws SGSException;

    void deleteAcademyClass(Long id);

    List<AcademyClass> getAcademyClasses();

    List<AcademyClass> getAcademyClasses(String username, String queryKey) throws SGSException;

    AcademyClass findAcademyClassById(Long id);

    void attachStudentsToAcademyClass(Long academyClassId, List<Long> studentIdList) throws SGSException;

    void attachSubjectsToAcademyClass(Long academyClassId, List<Long> subjectIdList) throws SGSException;
}
