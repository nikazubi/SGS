package mthiebi.sgs.service;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.SystemUser;

import java.util.List;
import java.util.Optional;

public interface SystemUserService {

    SystemUser createSystemUser(SystemUser systemUser) throws SGSException;

    SystemUser updateUser(SystemUser systemUser) throws SGSException;

    List<SystemUser> filterUsers(String username, String name, Boolean active);

    SystemUser findById(long id) throws SGSException;

    SystemUser delete(long userId) throws SGSException;

    SystemUser changeActivity(long id) throws SGSException;

}
