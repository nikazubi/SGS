package mthiebi.sgs.controllers;

import lombok.extern.slf4j.Slf4j;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.SystemUserGroup;
import mthiebi.sgs.service.SystemGroupService;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system-user-group")
@Slf4j
public class SystemGroupController {

    @Autowired
    private SystemGroupService systemGroupService;

    @GetMapping("/get-all")
    @Secured({AuthConstants.VIEW_SYSTEM_USER_GROUP})
    public List<SystemUserGroup> getSystemGroups() {
        return systemGroupService.getAll();
    }

    @GetMapping("/filter")
    @Secured({AuthConstants.VIEW_SYSTEM_USER_GROUP})
    public List<SystemUserGroup> filterSystemGroups(@RequestParam(required = false) String name,
                                                    @RequestParam(required = false) String permission) {
        return systemGroupService.getByNameAndPermission(name, permission);
    }

    @PutMapping("/edit")
    @Secured({AuthConstants.VIEW_SYSTEM_USER_GROUP})
    public SystemUserGroup editSystemGroups(@RequestBody SystemUserGroup systemUserGroup) throws SGSException {
        return systemGroupService.updateSystemUserGroup(systemUserGroup);
    }

    @PostMapping("/create")
    @Secured({AuthConstants.VIEW_SYSTEM_USER_GROUP})
    public SystemUserGroup createSystemGroups(@RequestBody SystemUserGroup systemUserGroup) throws SGSException {
        return systemGroupService.createSystemUserGroup(systemUserGroup);
    }

    @DeleteMapping("/delete/{id}")
    @Secured({AuthConstants.VIEW_SYSTEM_USER_GROUP})
    public void deleteSystemGroups(@PathVariable Long id) throws SGSException {
        systemGroupService.deleteSystemUserGroup(id);
    }
}
