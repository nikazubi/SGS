package mthiebi.sgs.repository;

import mthiebi.sgs.models.SystemUserGroup;

import java.util.List;

public interface SystemUserGroupRepositoryCustom {

    List<SystemUserGroup> findByNameAndPermission(String name, String permission);
}
