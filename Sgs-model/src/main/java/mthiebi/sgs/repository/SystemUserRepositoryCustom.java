package mthiebi.sgs.repository;

import mthiebi.sgs.models.SystemUser;

import java.util.List;

public interface SystemUserRepositoryCustom {

    List<SystemUser> filter(String username, String name, Boolean active);

}
