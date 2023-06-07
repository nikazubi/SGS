package mthiebi.sgs.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class UtilsJwt implements Serializable {

	private static final long serialVersionUID = 234234523523L;

	public static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60;

	private static final String ROLES_CLAIM_NAME = "scp";

	@Value("${jwt.secret}")
	private String secretKey;

	public String getUsernameFromToken(String token) {
		return getClaimFromToken(token, Claims::getSubject);
	}


	public Date getExpirationDateFromToken(String token) {
		return getClaimFromToken(token, Claims::getExpiration);
	}


	public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = getAllClaimsFromToken(token);
		return claimsResolver.apply(claims);
	}

	private Claims getAllClaimsFromToken(String token) {
		return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
	}

	private Boolean isTokenExpired(String token) {
		final Date expiration = getExpirationDateFromToken(token);
		return expiration.before(new Date());
	}

	public String generateToken(String authorities, String subject) {
		return Jwts.builder()
			.setSubject(subject)
			.claim(ROLES_CLAIM_NAME, authorities)
			.signWith(SignatureAlgorithm.HS512, secretKey)
			.setIssuedAt(new Date(System.currentTimeMillis()))
			.setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
			.compact();
	}

	public String generateToken(Authentication authentication) {
		final String authorities = authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.joining(","));
		return Jwts.builder()
			.setSubject(authentication.getName())
			.claim(ROLES_CLAIM_NAME, authorities)
			.signWith(SignatureAlgorithm.HS512, secretKey)
			.setIssuedAt(new Date(System.currentTimeMillis()))
			.setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
			.compact();
	}


	public Boolean validateToken(String token, UserDetails userDetails) {
		final String username = getUsernameFromToken(token);
		return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
	}

	public UsernamePasswordAuthenticationToken getAuthentication(final String token, final Authentication existingAuth, final UserDetails userDetails) {
		final JwtParser jwtParser = Jwts.parser().setSigningKey(secretKey);

		final Jws claimsJws = jwtParser.parseClaimsJws(token);

		final Claims claims = (Claims) claimsJws.getBody();
		String rolesString = claims.get(ROLES_CLAIM_NAME).toString();
		String[] roles = rolesString.equals("") ? new String[0] : rolesString.split(",");
		final Collection<SimpleGrantedAuthority> authorities =
			Arrays.stream(roles)
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());

		return new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(), authorities);
	}
}