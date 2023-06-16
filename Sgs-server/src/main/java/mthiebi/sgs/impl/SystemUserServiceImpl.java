package mthiebi.sgs.impl;

import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.repository.SystemUserRepository;
import mthiebi.sgs.service.SystemUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class SystemUserServiceImpl implements SystemUserService {

    private static final Logger logger = LoggerFactory.getLogger(SystemUserServiceImpl.class);

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public SystemUser createSystemUser(SystemUser systemUser) throws Exception {
        if (true) {
            return null;
        } else {
//            encryptPassword(systemUser); todo
            systemUserRepository.save(systemUser);
            return systemUser;
        }
    }

    private void encryptPassword(SystemUser systemUser) {
        systemUser.setPassword(passwordEncoder.encode(systemUser.getPassword()));
    }

    @Override
    @Transactional
    public SystemUser updateUser(SystemUser systemUser) throws Exception {
        Optional<SystemUser> systemUserOptional = systemUserRepository.findById(systemUser.getId());
        if (systemUserOptional.isPresent()) {
            SystemUser user = systemUserOptional.get();
            if (systemUser.getUsername().isEmpty() ||
                    systemUser.getName().isEmpty() ||
                    systemUser.getEmail().isEmpty()) {

                logger.info("Need to fill all fields");
                throw new Exception("UsersFillAllFields");
            } else if (systemUser.getGroups().size() < 1) {
                logger.info("Need to choose min 1 groups");
                throw new Exception("UsersGroupValidation");
            } else if (systemUserRepository.findSystemUserByUsername(systemUser.getUsername()) != null && !systemUser.getUsername().equals(user.getUsername())) {
                logger.info("This username is already taken");
                throw new Exception("UsersUsernameUniqValidation");
            } else {
                if (systemUser.getPassword() != null) {
//                    encryptPassword(systemUser);
                    user.setPassword(systemUser.getPassword());
                }
                user.setName(systemUser.getName());
                user.setUsername(systemUser.getUsername());
                user.setEmail(systemUser.getEmail());
                user.setGroups(systemUser.getGroups());
                return systemUserRepository.findById(systemUserOptional.get().getId()).orElseThrow();
            }
        } else {
            logger.info("User not found");
            throw new Exception("UserNotFound");
        }
    }


//    @Override
//    public List<SystemUser> filterUsers(int limit, int page, String username, String name, String groupName, String active) {
//        return systemUserRepository.filterUser(limit, page, username, name, groupName, active, em);
//    }

    @Override
    public SystemUser findById(long id) throws Exception {
        Optional<SystemUser> systemUserOptional = systemUserRepository.findById(id);
        if (systemUserOptional.isPresent()) {
            logger.info("User found successfully");
            return systemUserOptional.get();
        } else {
            logger.info("User not found");
            throw new Exception("UserNotFound");
        }
    }


    @Override
    public SystemUser delete(long userId) throws Exception {
        Optional<SystemUser> systemUserOptional = systemUserRepository.findById(userId);
        if (systemUserOptional.isPresent()) {
            systemUserRepository.delete(systemUserOptional.get());
            return systemUserOptional.get();
        } else {
            logger.info("User not found");
            throw new Exception("UserNotFound");
        }
    }

    @Override
    @Transactional
    public SystemUser changeActivity(long id) throws Exception {
        Optional<SystemUser> systemUserOptional = systemUserRepository.findById(id);
        if (systemUserOptional.isPresent()) {
            SystemUser user = systemUserOptional.get();
            user.setActive(!user.getActive());
            return findById(user.getId());
        } else {
            logger.info("User not found");
            throw new Exception("UserNotFound");
        }
    }

}
