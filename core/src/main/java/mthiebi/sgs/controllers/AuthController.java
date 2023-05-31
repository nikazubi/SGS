package mthiebi.sgs.controllers;


import lombok.extern.slf4j.Slf4j;
import mthiebi.sgs.jwtmodels.JwtRequest;
import mthiebi.sgs.jwtmodels.JwtResponse;
import mthiebi.sgs.utils.UtilsJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@Slf4j
public class AuthController {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	UserDetailsService userDetailsService;

	@Autowired
	private UtilsJwt utilsJwt;

	@PostMapping("/authenticate")
	public JwtResponse authenticate(@RequestBody JwtRequest jwtRequest) throws Exception {
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
			throw new Exception("INVALID_CREDENTIALS", e);
		}

		final String token = utilsJwt.generateToken(authentication);
		log.info("jwt token is: " + token);
		return new JwtResponse(token);
	}

	@PostMapping("/refresh-token")
	public JwtResponse refreshToken(@RequestHeader("Authorization") String authHeader) throws Exception {
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
		throw new Exception("INVALID JWT");
	}
}

