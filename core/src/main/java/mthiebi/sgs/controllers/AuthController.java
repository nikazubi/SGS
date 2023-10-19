package mthiebi.sgs.controllers;


import lombok.extern.slf4j.Slf4j;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.dto.StudentMapper;
import mthiebi.sgs.dto.UserAndPermissionDTO;
import mthiebi.sgs.jwtmodels.ConsoleJwtResponse;
import mthiebi.sgs.jwtmodels.JwtRequest;
import mthiebi.sgs.jwtmodels.JwtResponse;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.repository.StudentRepository;
import mthiebi.sgs.utils.UtilsJwt;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@Slf4j
public class AuthController {

    @Autowired
    UserDetailsService userDetailsService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private UtilsJwt utilsJwt;

    @PostMapping("/authenticate")
    public JwtResponse authenticate(@RequestBody JwtRequest jwtRequest) throws SGSException {
        String username = jwtRequest.getUsername();
        String password = jwtRequest.getPassword();
        log.info("request arrived at /authenticate with username: " +
                username + " and password: " + password);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            jwtRequest.getUsername(),
                            jwtRequest.getPassword()
                    )
            );

        } catch (BadCredentialsException e) {
            log.info("username and password not correct, throw invalid credentials exception");
            throw new SGSException("INVALID_CREDENTIALS");
        }

        final String token = utilsJwt.generateToken(authentication);
        log.info("jwt token is: " + token);
        return new JwtResponse(token);
    }

    @PostMapping("/authenticate-student")
    public ConsoleJwtResponse authenticateStudent(@RequestBody JwtRequest jwtRequest) throws SGSException {
        String username = jwtRequest.getUsername();
        String encodedPassword = DigestUtils.md5Hex(jwtRequest.getPassword()).toUpperCase();

        log.info("request arrived at /authenticate with username: " +
                username + " and password: " + jwtRequest.getPassword());
        log.info("encoded password: {}", encodedPassword);
        Student student;
        try {
            student = studentRepository.authStudent(username, encodedPassword);
            if (student == null) {
                throw new SGSException("INVALID_CREDENTIALS");
            }
        } catch (BadCredentialsException e) {
            log.info("username and password not correct, throw invalid credentials exception");
            throw new SGSException("INVALID_CREDENTIALS");
        }

        final String token = utilsJwt.generateTokenForStudent(student.getUsername());
        log.info("jwt token is: " + token);
        return new ConsoleJwtResponse(token, studentMapper.studentDTO(student));
    }

    @PostMapping("/refresh-token")
    public JwtResponse refreshToken(@RequestHeader("authorization") String authHeader) throws SGSException {
        if (authHeader.startsWith("Bearer ")) {
            String jwtToken = authHeader.substring(7);
            String userName = utilsJwt.getUsernameFromToken(jwtToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userName);

            if (utilsJwt.validateToken(jwtToken, userDetails)) {
                String authorities = userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(","));
                String updatedToken = utilsJwt.generateToken(authorities, userDetails.getUsername());
                return new JwtResponse(updatedToken);
            }
        }
        throw new SGSException("INVALID JWT");
    }

    @GetMapping("/user-and-permissions")
    public UserAndPermissionDTO getUserAndPermissions(@RequestHeader("authorization") String authHeader) throws Exception {
        UserAndPermissionDTO userAndPermissionDTO = new UserAndPermissionDTO();
        userAndPermissionDTO.setUsername(utilsJwt.getUsernameFromHeader(authHeader));
        userAndPermissionDTO.setPermissionList(utilsJwt.getAuthoritiesFromToken(authHeader));
        return userAndPermissionDTO;
    }
}

