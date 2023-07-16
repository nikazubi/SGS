package mthiebi.sgs.controllers;

import lombok.extern.slf4j.Slf4j;
import mthiebi.sgs.models.SystemUserGroup;
import mthiebi.sgs.service.SystemGroupService;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/system-user-group")
@Slf4j
public class SystemGroupController {

    @Autowired
    private SystemGroupService systemGroupService;

    @GetMapping("/get-all") // todo: change to filter
    @Secured({AuthConstants.MANAGE_GRADES}) //todo
    public List<SystemUserGroup> getSystemGroups(){
        return systemGroupService.getAll();
    }
}
