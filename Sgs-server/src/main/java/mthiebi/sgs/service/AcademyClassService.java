package mthiebi.sgs.service;

import java.util.List;
import mthiebi.sgs.models.AcademyClass;

public interface AcademyClassService {

    AcademyClass createAcademyClass(AcademyClass academyClass);

    AcademyClass updateAcademyClass(AcademyClass academyClass);

    void deleteAcademyClass(Long id);

    List<AcademyClass> getAcademyClasses();

    AcademyClass findAcademyClassById(Long id);

    void attachStudentsToAcademyClass(Long academyClassId, List<Long> studentIdList);

    void attachSubjectsToAcademyClass(Long academyClassId, List<Long> subjectIdList);
}
