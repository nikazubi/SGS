package mthiebi.sgs.service;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.SystemUserGroup;

import java.util.List;
import java.util.Optional;

public interface SystemGroupService {

    List<SystemUserGroup> getAll();

    SystemUserGroup getById(Long id);

    SystemUserGroup createSystemUserGroup(SystemUserGroup userGroup) throws SGSException;

    SystemUserGroup updateSystemUserGroup(SystemUserGroup userGroup) throws SGSException;

    boolean deleteSystemUserGroup(long id) throws SGSException;

//    List<SystemUserGroup> filterSystemUserGroup(String name, String permission, String active);
//
}
