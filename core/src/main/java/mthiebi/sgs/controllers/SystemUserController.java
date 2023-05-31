package mthiebi.sgs.controllers;

import lombok.extern.slf4j.Slf4j;
import mthiebi.sgs.dto.SystemUserCreateDTO;
import mthiebi.sgs.dto.SystemUserDTO;
import mthiebi.sgs.dto.SystemUserMapper;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.models.SystemUserGroup;
import mthiebi.sgs.service.SystemGroupService;
import mthiebi.sgs.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/system-user")
@Slf4j
public class SystemUserController {

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private SystemUserMapper systemUserMapper;

    @Autowired
    private SystemGroupService systemGroupService;

//    @GetMapping("/")
//    public List<SystemUserDTO> getUsers(@RequestParam(required = false, defaultValue = "50") String limit,
//                                        @RequestParam(required = false, defaultValue = "0") String page,
//                                        @RequestParam(required = false, defaultValue = "") String username,
//                                        @RequestParam(required = false, defaultValue = "") String name,
//                                        @RequestParam(required = false, defaultValue = "") String groupName,
//                                        @RequestParam(required = false, defaultValue = "") String active) {
//
//        log.info("required filtration: username=\"" + username + "\" name=\"" + name + "\" groupName=\"" + groupName + "\" active=\"" + active + "\"");
//        return systemUserService.filterUsers(Integer.parseInt(limit),Integer.parseInt(page),username, name, groupName, active).stream()
//                .map(SystemUserDTO::new).collect(Collectors.toList());
//    }

//    @GetMapping("/usersCounts")
//    public int getUsersCounts(@RequestParam(required = false, defaultValue = "") String username,
//                              @RequestParam(required = false, defaultValue = "") String name,
//                              @RequestParam(required = false, defaultValue = "") String groupName,
//                              @RequestParam(required = false, defaultValue = "") String active){
//        return systemUserService.getUsersCounts(username, name, groupName, active);
//    }

    @PostMapping("/add-User")
    public ResponseEntity create(@RequestBody SystemUserCreateDTO systemUserCreateDTO) {
        SystemUser systemUser = systemUserMapper.systemUser(systemUserCreateDTO.getSystemUserDTO());
        systemUser.setGroups(adjustSystemGroup(systemUserCreateDTO.getGroupIdList()));
        log.info("Add new user:" + systemUser);
        try {
            systemUser = systemUserService.createSystemUser(systemUser);
            log.info("New User successfully created");
            return ResponseEntity.ok(systemUserMapper.systemUserDTO(systemUser));
        } catch (Exception e) {
            log.info("Error in create controller");
            log.info(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/statuschange/{userId}")
    public ResponseEntity changeActiveStatus(@PathVariable long userId) {
        log.info("Change user status: userId=\"" + userId + "\"");
        try {
            SystemUser systemUser = systemUserService.findById(userId);
            log.info("before changes: active=" + systemUser.getActive());
            systemUser = systemUserService.changeActivity(userId);
            log.info("after changes: active=" + systemUser.getActive());
            return ResponseEntity.ok(systemUserMapper.systemUserDTO(systemUser));
        } catch (Exception e) {
            log.info("Error in changeActiveStatus controller");
            log.info(e.getMessage());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(e.getMessage());
        }
    }

    @PutMapping("/update")
    public ResponseEntity updateUser(@RequestBody SystemUserCreateDTO systemUserCreateDTO) {
        SystemUser systemUser = systemUserMapper.systemUser(systemUserCreateDTO.getSystemUserDTO());
        systemUser.setGroups(adjustSystemGroup(systemUserCreateDTO.getGroupIdList()));
        log.info("Starting user update");
        try {
            log.info("Required Changes=" + systemUser + " \nTo user: " + systemUserService.findById(systemUser.getId()));
            SystemUser sysUser = systemUserService.updateUser(systemUser);
            log.info("User successfully changed");
            return ResponseEntity.ok(systemUserMapper.systemUserDTO(sysUser));
        } catch (Exception e) {
            log.info("Error in updateUser controller");
            log.info(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }


    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity delete(@PathVariable long userId) {
        try {
            log.info("required delete user: " + systemUserService.findById(userId));
            systemUserService.delete(userId);
            log.info("User successfully deleted");
            return ResponseEntity.ok(new SystemUserDTO());
        } catch (Exception e) {
            log.info("Error in delete controller");
            log.info(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    private List<SystemUserGroup> adjustSystemGroup(List<Long> groupIdList) {
        return groupIdList.stream()
                .map(id -> systemGroupService.getById(id))
                .collect(Collectors.toList());
    }

}
