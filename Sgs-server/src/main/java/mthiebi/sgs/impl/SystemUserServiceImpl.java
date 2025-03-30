package mthiebi.sgs.impl;

import mthiebi.sgs.ExceptionKeys;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.repository.SystemUserRepository;
import mthiebi.sgs.service.SystemUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public SystemUser createSystemUser(SystemUser systemUser) throws SGSException {
//            encryptPassword(systemUser); todo
        if (systemUser.getUsername().isEmpty() ||
                systemUser.getName().isEmpty() ||
                systemUser.getPassword().isEmpty()) {
            logger.info("Need to fill all fields");
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SYSTEM_USER_FIELDS_INVALID);
        }
        try {
            systemUserRepository.save(systemUser);
        } catch (DataIntegrityViolationException ex) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SYSTEM_USER_USERNAME_TAKEN);
        }
        return systemUser;
    }

    private void encryptPassword(SystemUser systemUser) {
        systemUser.setPassword(passwordEncoder.encode(systemUser.getPassword()));
    }

    @Override
    @Transactional
    public SystemUser updateUser(SystemUser systemUser) throws SGSException {
        Optional<SystemUser> systemUserOptional = systemUserRepository.findById(systemUser.getId());
        if (systemUserOptional.isPresent()) {
            SystemUser user = systemUserOptional.get();
            if (systemUser.getUsername().isEmpty() ||
                    systemUser.getName().isEmpty()) {

                logger.info("Need to fill all fields");
                throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SYSTEM_USER_FIELDS_INVALID);
            } else {
                if (systemUser.getPassword() != null) {
//                    encryptPassword(systemUser);
                    user.setPassword(systemUser.getPassword());
                }
                user.setName(systemUser.getName());
                user.setUsername(systemUser.getUsername());
                user.setEmail(systemUser.getEmail());
                user.setGroups(systemUser.getGroups());
                user.setAcademyClassList(systemUser.getAcademyClassList());
                return systemUserRepository.findById(systemUserOptional.get().getId()).orElseThrow();
            }
        } else {
            logger.info("User not found");
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SYSTEM_USER_NOT_FOUND);
        }
    }


    @Override
    public List<SystemUser> filterUsers(String username, String name, Boolean active) {
        return systemUserRepository.filter(username, name, active);
    }

    @Override
    public SystemUser findById(long id) throws SGSException {
        Optional<SystemUser> systemUserOptional = systemUserRepository.findById(id);
        if (systemUserOptional.isPresent()) {
            logger.info("User found successfully");
            return systemUserOptional.get();
        } else {
            logger.info("User not found");
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SYSTEM_USER_NOT_FOUND);
        }
    }


    @Override
    public SystemUser delete(long userId) throws SGSException {
        Optional<SystemUser> systemUserOptional = systemUserRepository.findById(userId);
        if (systemUserOptional.isPresent()) {
            try {
                systemUserRepository.delete(systemUserOptional.get());
            } catch (Exception e) {
                throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.ERASE_ALL_DATA_FROM_SYSTEM_USER);
            }
            return systemUserOptional.get();
        } else {
            logger.info("User not found");
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SYSTEM_USER_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public SystemUser changeActivity(long id) throws SGSException {
        Optional<SystemUser> systemUserOptional = systemUserRepository.findById(id);
        if (systemUserOptional.isPresent()) {
            SystemUser user = systemUserOptional.get();
            user.setActive(!user.getActive());
            return findById(user.getId());
        } else {
            logger.info("User not found");
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SYSTEM_USER_NOT_FOUND);
        }
    }

}
