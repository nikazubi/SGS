package mthiebi.sgs.repository;

import mthiebi.sgs.models.AcademyClass;

import java.util.List;

public interface AcademyClassRepositoryCustom {

    List<AcademyClass> getAcademyClasses(String queryKey);
}
