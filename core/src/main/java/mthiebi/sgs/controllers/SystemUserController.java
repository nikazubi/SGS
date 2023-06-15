package mthiebi.sgs.controllers;

import lombok.extern.slf4j.Slf4j;
import mthiebi.sgs.dto.SystemUserCreateDTO;
import mthiebi.sgs.dto.SystemUserDTO;
import mthiebi.sgs.dto.SystemUserMapper;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.models.SystemUserGroup;
import mthiebi.sgs.service.AcademyClassService;
import mthiebi.sgs.service.SystemGroupService;
import mthiebi.sgs.service.SystemUserService;
import mthiebi.sgs.utils.AuthConstants;
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

    @Autowired
    private AcademyClassService academyClassService;

    @PostMapping("/add-User")
    @Secured({AuthConstants.MANAGE_SYSTEM_USER})
    public ResponseEntity create(@RequestBody SystemUserCreateDTO systemUserCreateDTO) {
        SystemUser systemUser = systemUserMapper.systemUser(systemUserCreateDTO.getSystemUserDTO());
        systemUser.setGroups(adjustSystemGroup(systemUserCreateDTO.getGroupIdList()));
        systemUser.setAcademyClassList(adjustAcademyClassList(systemUserCreateDTO.getClassIdList()));
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
    @Secured({AuthConstants.MANAGE_SYSTEM_USER})
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
    @Secured({AuthConstants.MANAGE_SYSTEM_USER})
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
    @Secured({AuthConstants.MANAGE_SYSTEM_USER})
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

    private List<AcademyClass> adjustAcademyClassList(List<Long> classIdList) {
        return classIdList.stream()
                .map(id -> academyClassService.findAcademyClassById(id))
                .collect(Collectors.toList());
    }

}
