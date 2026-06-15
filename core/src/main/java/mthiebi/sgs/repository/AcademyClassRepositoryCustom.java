package mthiebi.sgs.repository;

import mthiebi.sgs.models.AcademyClass;

import java.util.List;
import java.util.Optional;

public interface AcademyClassRepositoryCustom {

    List<AcademyClass> getAcademyClasses(List<AcademyClass> academyClassList, String queryKey);

    List<AcademyClass> getAcademyClasses(String queryKey);

    Optional<AcademyClass> getAcademyClassByStudent(long studentId);
}
