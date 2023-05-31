package mthiebi.sgs.service;

import mthiebi.sgs.models.SystemUser;

import java.util.List;
import java.util.Optional;

public interface SystemUserService {

    SystemUser createSystemUser(SystemUser systemUser) throws Exception;

    SystemUser updateUser(SystemUser systemUser) throws Exception;

//    List<SystemUser> filterUsers(int limit, int page, String username, String name, String group, String active);

    SystemUser findById(long id) throws Exception;

    SystemUser delete(long userId) throws Exception;

    SystemUser changeActivity(long id) throws Exception;

}
