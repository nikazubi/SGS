package mthiebi.sgs.service;

import mthiebi.sgs.models.SystemUserGroup;

import java.util.List;
import java.util.Optional;

public interface SystemGroupService {

    List<SystemUserGroup> getAll();

    SystemUserGroup getById(Long id);

    SystemUserGroup createSystemUserGroup(SystemUserGroup userGroup) throws Exception;

    SystemUserGroup updateSystemUserGroup(SystemUserGroup userGroup) throws Exception;

    boolean deleteSystemUserGroup(long id) throws Exception;

//    List<SystemUserGroup> filterSystemUserGroup(String name, String permission, String active);
//
}
