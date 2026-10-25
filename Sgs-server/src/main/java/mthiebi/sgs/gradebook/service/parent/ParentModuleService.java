package mthiebi.sgs.gradebook.service.parent;

import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.model.ParentModule;
import mthiebi.sgs.gradebook.repository.ParentModuleGrantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Which modules a child's parent is shown.
 * <p>
 * The one owner of that answer. The landing page, every content endpoint's
 * guard and the journal list all ask here, so a module cannot be offered by
 * one and refused by another.
 * <p>
 * The answer is data - {@code sgs.parent_module_grant}, seeded by db/038 - and
 * not a rule in code. It used to be "primary gets the schedule, the menu and the
 * description, and no grades", written out in two services; then the school
 * wanted grades 5 and 6 of primary to keep the primary pages and have grades as
 * well. That kind of change is now a row.
 */
@Service
public class ParentModuleService {

    @Autowired
    private ParentModuleGrantRepository grantRepository;

    /**
     * In {@link ParentModule} declaration order, which is landing-page order.
     */
    @Transactional(readOnly = true)
    public List<ParentModule> modulesFor(Enrollment enrollment) {
        ClassGroup classGroup = enrollment.getClassGroup();
        Long schoolId = classGroup.getSchool() == null ? null : classGroup.getSchool().getId();

        return grantRepository.findModulesFor(schoolId, classGroup.getLevel()).stream()
                .sorted()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean has(Enrollment enrollment, ParentModule module) {
        return modulesFor(enrollment).contains(module);
    }
}
