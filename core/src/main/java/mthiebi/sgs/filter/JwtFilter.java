package mthiebi.sgs.filter;


import lombok.extern.slf4j.Slf4j;
import mthiebi.sgs.configuration.security.UserDetailsServiceImpl;
import mthiebi.sgs.utils.UtilsJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

	@Autowired
	private UserDetailsServiceImpl userService;

	@Autowired
	private UtilsJwt utilsJwt;

	@Override
	protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
		String authorization = httpServletRequest.getHeader("authorization");
		String token = null;
		String userName = null;
		log.info("request arrived at jwt filter, authorization header is: " + authorization);

		if (null != authorization && authorization.startsWith("Bearer ")) {
			token = authorization.substring(7);
            try {
                userName = utilsJwt.getUsernameFromToken(token);
            } catch (Exception e) {
                // A malformed, expired or forged token is a refusal, not a
                // server error. Letting the parse throw here produced a 500 and
                // a stack trace for anything that was not a valid token.
                log.info("unusable token: {}", e.getClass().getSimpleName());
                userName = null;
            }
		}

		log.info("userName is: " + userName);
		if (null != userName && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails;
            try {
                userDetails = userService.loadUserByUsername(userName, httpServletRequest);
            } catch (RuntimeException e) {
                // The subject named nobody. Leaving the context unauthenticated
                // lets the chain answer 403; throwing would answer 500.
                log.info("token subject not found: {}", userName);
                filterChain.doFilter(httpServletRequest, httpServletResponse);
                return;
            }
			if (utilsJwt.validateToken(token, userDetails) && userDetails.isEnabled()) {
				UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
					= utilsJwt.getAuthentication(token, SecurityContextHolder.getContext().getAuthentication(), userDetails);

				usernamePasswordAuthenticationToken.setDetails(
					new WebAuthenticationDetailsSource().buildDetails(httpServletRequest)
				);

				SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
				log.info("jwt authorization completed successful");
			}
		}
		filterChain.doFilter(httpServletRequest, httpServletResponse);
	}
}