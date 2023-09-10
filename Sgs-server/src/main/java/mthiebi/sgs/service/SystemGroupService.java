package mthiebi.sgs.service;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.SystemUserGroup;

import java.util.List;

public interface SystemGroupService {

    List<SystemUserGroup> getAll();

    List<SystemUserGroup> getByNameAndPermission(String name, String permission);

    SystemUserGroup getById(Long id);

    SystemUserGroup createSystemUserGroup(SystemUserGroup userGroup) throws SGSException;

    SystemUserGroup updateSystemUserGroup(SystemUserGroup userGroup) throws SGSException;

    boolean deleteSystemUserGroup(long id) throws SGSException;

//    List<SystemUserGroup> filterSystemUserGroup(String name, String permission, String active);
//
}
