package mthiebi.sgs.impl;

import mthiebi.sgs.models.SystemUserGroup;
import mthiebi.sgs.repository.SystemGroupRepository;
import mthiebi.sgs.service.SystemGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
public class SystemGroupServiceImpl implements SystemGroupService {

    @Autowired
    private SystemGroupRepository systemGroupRepository;
    
    public List<SystemUserGroup> getAll(){
        return systemGroupRepository.findAll();
    }

    @Override
    public SystemUserGroup getById(Long id){
        return systemGroupRepository.findById(id).orElseThrow();
    }

    @Override
    public SystemUserGroup createSystemUserGroup(SystemUserGroup SystemUserGroup) throws Exception {
        return systemGroupRepository.save(SystemUserGroup);
    }

    @Override
    @Transactional
    public SystemUserGroup updateSystemUserGroup(SystemUserGroup SystemUserGroup) throws Exception {
        SystemUserGroup foundSystemUserGroup = getById(SystemUserGroup.getId());

        foundSystemUserGroup.setId(SystemUserGroup.getId());
        foundSystemUserGroup.setName(SystemUserGroup.getName());
        foundSystemUserGroup.setPermissions(SystemUserGroup.getPermissions());
        foundSystemUserGroup.setActive(SystemUserGroup.getActive());

        return getById(SystemUserGroup.getId());
    }

    public boolean deleteSystemUserGroup(long id) throws Exception{
        systemGroupRepository.deleteById(id);
        return true;
    }

//    public List<SystemUserGroup> filterSystemUserGroup(String name, String permission, String active){
//        return systemGroupRepository.filterSystemUserGroup(name, permission, active, em);
//    }

}
