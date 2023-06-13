package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.QAcademyClass;
import mthiebi.sgs.models.QStudent;
import mthiebi.sgs.models.Student;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
public interface AcademyClassRepository extends JpaRepository<AcademyClass, Long>,
                                                QuerydslPredicateExecutor<AcademyClass>,
                                                            AcademyClassRepositoryCustom{


}
