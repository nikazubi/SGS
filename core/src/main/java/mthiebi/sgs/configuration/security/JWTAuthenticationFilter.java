package mthiebi.sgs.configuration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mthiebi.sgs.auth.AuthManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

//	private static final ObjectMapper objectMapper = new ObjectMapper();
//
//	@Autowired
//	private AuthManager authManager;
//
//	@Autowired
//	private SystemUsersService systemUsersService;
//
//	@Autowired
//	private SystemUserAuthMapper systemUserAuthMapper;
//
//	public JWTAuthenticationFilter(String fitlterProcesessUr, AuthenticationManager authenticationManager) {
//		super.setAuthenticationManager(authenticationManager);
//		setFilterProcessesUrl(fitlterProcesessUr);
//	}
//
//	@Override
//	public Authentication attemptAuthentication(HttpServletRequest req,
//												HttpServletResponse res) throws AuthenticationException {
//		try {
//			AuthenticationRequest authenticationRequest = objectMapper
//				.readValue(req.getInputStream(), AuthenticationRequest.class);
//			Authentication authenticationToken = generateAuthenticationToken(authenticationRequest);
//
//			return getAuthenticationManager().authenticate(authenticationToken);
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	@Override
//	protected void successfulAuthentication(HttpServletRequest req,
//											HttpServletResponse res,
//											FilterChain chain,
//											Authentication auth) throws IOException {
//		String username = auth.getPrincipal().toString();
//		SystemUser systemUser = systemUsersService.getSystemUserWithConfig(username).get();
//
//		String accessToken = authManager.createAccessToken(systemUser);
//		String refreshToken = authManager.createRefreshToken(systemUser);
//		SystemUserDto userDto = systemUserAuthMapper.systemUserDto(systemUser);
//		AuthenticationResult authenticationResult = new AuthenticationResult(userDto, accessToken, refreshToken, false);
//
//		String body = objectMapper.writeValueAsString(authenticationResult);
//
//		res.getWriter().write(body);
//		res.getWriter().flush();
//	}
//
//	@Override
//	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
//		response.sendError(HttpStatus.UNAUTHORIZED.value(), failed.getMessage());
//	}
//
//	private Authentication generateAuthenticationToken(AuthenticationRequest authenticationRequest) {
//		String username = authenticationRequest.getUsername();
//		String password = authenticationRequest.getPassword();
//		String otp = authenticationRequest.getOtp();
//		Long authenticationProviderCompanyId = authenticationRequest.getAuthenticationProviderCompanyId();
//		Authentication authentication;
//		if (authenticationProviderCompanyId == null) {
//			authentication = new UserNamePasswordOtpAuthenticationToken(username, password, otp);
//		} else {
//			authentication = new LdapUsernamePasswordOtpAuthenticationToken(username, password, otp, authenticationProviderCompanyId);
//		}
//		return authentication;
//	}
}