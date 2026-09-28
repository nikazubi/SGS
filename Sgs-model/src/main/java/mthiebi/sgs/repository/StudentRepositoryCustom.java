package mthiebi.sgs.repository;

import mthiebi.sgs.models.Student;

import java.util.Optional;

/**
 * What is left of the legacy dbo.students queries: the two the legacy student
 * login still uses. Everything that went through dbo.academy_class is gone -
 * classes live only in sgs.class_group.
 */
public interface StudentRepositoryCustom {

    Student authStudent(String username, String password);

    Optional<Student> findByUsername(String username);

}
