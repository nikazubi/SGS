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
		String authorization = httpServletRequest.getHeader("Authorization");
		String token = null;
		String userName = null;
		log.info("request arrived at jwt filter, authorization header is: " + authorization);

		if (null != authorization && authorization.startsWith("Bearer ")) {
			token = authorization.substring(7);
			userName = utilsJwt.getUsernameFromToken(token);
		}

		log.info("userName is: " + userName);
		if (null != userName && SecurityContextHolder.getContext().getAuthentication() == null) {
			UserDetails userDetails = userService.loadUserByUsername(userName);
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