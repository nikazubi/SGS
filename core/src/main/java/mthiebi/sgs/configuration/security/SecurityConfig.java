package mthiebi.sgs.configuration.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
//import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableGlobalMethodSecurity(
	securedEnabled = true,
	prePostEnabled = true
)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

//	@Autowired
//	private OAuthLoginSuccessHandler successHandler;
//
//	@Autowired
//	private LocalAuthenticationProvider localAuthenticationProvider;
//
//	@Autowired
//	private CustomLdapAuthenticationProvider customLdapAuthenticationProvider;
//
//	@Override
//	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//		auth.authenticationProvider(localAuthenticationProvider)
//			.authenticationProvider(customLdapAuthenticationProvider);
//	}
//
//	@Bean
//	public AuthenticationManager authenticationManagerBean() throws Exception {
//		return super.authenticationManagerBean();
//	}
//
//	@Bean
//	JWTAuthenticationFilter jwtAuthenticationFilter() throws Exception {
//		return new JWTAuthenticationFilter("/console/authentication", authenticationManagerBean());
//	}
//
//	@Override
//	protected void configure(HttpSecurity http) throws Exception {
//		http
//			.cors()
//			.and()
//			.csrf().disable()
//			.oauth2Login()
//			.authorizationEndpoint()
//			.baseUri("/portal/social/authorize")
//			.and()
//			.redirectionEndpoint()
//			.baseUri("/portal/social/callback/*")
//			.and()
//			.userInfoEndpoint()
//			.userService(new DefaultOAuth2UserService())
//			.and()
//			.successHandler(successHandler)
//			.and()
//			.httpBasic().disable()
//			.formLogin().disable()
//			.addFilter(jwtAuthenticationFilter())
//			.exceptionHandling()
//			.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
//			.and()
//			.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//			.and()
//			.authorizeRequests()
//			.antMatchers("/console/api/**").hasAuthority(AuthConstants.MANAGEMENT_CONSOLE_AUTHORITY)
//			.antMatchers("/portal/api/**").hasAuthority(AuthConstants.CUSTOMER_PORTAL_AUTHORITY)
//			.anyRequest().permitAll()
//			.and()
//			.oauth2ResourceServer()
//			.jwt()
//			.jwtAuthenticationConverter(jwtAuthenticationConverter());
//	}
//
//	@Bean
//	public PasswordEncoder passwordEncoder() {
//		return new BCryptPasswordEncoder();
//	}
//
//	@Bean
//	public GrantedAuthorityDefaults grantedAuthorityDefaults() {
//		return new GrantedAuthorityDefaults("");
//	}
//
//	private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
//		JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
//		grantedAuthoritiesConverter.setAuthorityPrefix("");
//		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
//		converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
//		return converter;
//	}

}
