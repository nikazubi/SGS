package mthiebi.sgs.auth;

import com.auth0.jwt.JWT;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.models.SystemUserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class AuthManager {

    public static final String USERNAME_CLAIM_NAME = "username_claim";
    public static final String EMAIL_CLAIM_NAME = "email_claim";
    public static final String LOGIN_CHANNEL_CLAIM = "channel_claim";
    private static final String ROLES_CLAIM_NAME = "scp";
    private static final String CARD_TYPE_ACCESS_CLAIM_NAME = "card_type_scp";
    private static final String CARD_TYPE_MANAGE_CLAIM_NAME = "card_type_manage_scp";
    private static final String COMPANY_ACCESS_CLAIM_NAME = "company_scp";
    private static final Integer consoleLifetime = 60;
    private static final Integer consoleRefreshLifetime = 180;
    private static final Integer portalTempLifetime = 30;
    private static final Integer portalLifetime = 120;
    private static final Integer portalRefreshLifetime = 120;

	@Autowired
	private RSAPublicKey publicKey;

	@Autowired
	private RSAPrivateKey privateKey;

//	@Autowired
//	private JwtProperties jwtProperties;

	public String createAccessToken(SystemUser systemUser) {
		Stream<String> permissionsStream = systemUser.getGroups().stream()
													 .map(SystemUserGroup::getListPermissions)
													 .flatMap(Collection::stream)
													 .distinct();

		String[] roles = Stream.concat(permissionsStream, Stream.of("ADMIN_CONSOLE"))
							   .toArray(String[]::new);

		return null;
//		return JWT.create()
//				  .withIssuer(jwtProperties.getIssuer())
//				  .withSubject(String.valueOf(systemUser.getId()))
//				  .withIssuedAt(new Date())
//				  .withExpiresAt(getExpirationTime(Calendar.MINUTE, consoleLifetime.value()))
//				  .withClaim(USERNAME_CLAIM_NAME, systemUser.getUsername())
//				  .withArrayClaim(ROLES_CLAIM_NAME, roles)
//				  .withArrayClaim(COMPANY_ACCESS_CLAIM_NAME, companyAccessPermissions)
//				  .withArrayClaim(CARD_TYPE_ACCESS_CLAIM_NAME, cardTypeAccessPermissions)
//                  .withArrayClaim(CARD_TYPE_MANAGE_CLAIM_NAME, cardTypeManagePermissions)
//				  .sign(Algorithm.RSA256(publicKey, privateKey));
	}

//	public String createAccessToken(Customer customer) {
//		return JWT.create()
//				  .withIssuer(jwtProperties.getIssuer())
//				  .withSubject(String.valueOf(customer.getId()))
//				  .withIssuedAt(new Date())
//				  .withExpiresAt(getExpirationTime(Calendar.MINUTE, portalLifetime.value()))
//				  .withArrayClaim(ROLES_CLAIM_NAME, new String[]{AuthConstants.CUSTOMER_PORTAL_AUTHORITY})
//				  .sign(Algorithm.RSA256(publicKey, privateKey));
//	}

//	public String createAccessTokenWithLoginChannel(Customer customer, String loginChannel) {
//		return JWT.create()
//				  .withIssuer(jwtProperties.getIssuer())
//				  .withSubject(String.valueOf(customer.getId()))
//				  .withIssuedAt(new Date())
//				  .withExpiresAt(getExpirationTime(Calendar.MINUTE, portalLifetime.value()))
//				  .withClaim(LOGIN_CHANNEL_CLAIM, loginChannel)
//				  .withArrayClaim(ROLES_CLAIM_NAME, new String[]{AuthConstants.CUSTOMER_PORTAL_AUTHORITY})
//				  .sign(Algorithm.RSA256(publicKey, privateKey));
//	}
//
//	public String createRefreshToken(SystemUser systemUser) {
//		return JWT.create()
//				  .withIssuer(jwtProperties.getIssuer())
//				  .withSubject(String.valueOf(systemUser.getId()))
//				  .withIssuedAt(new Date())
//				  .withExpiresAt(getExpirationTime(Calendar.MINUTE, consoleRefreshLifetime.value()))
//				  .withArrayClaim(ROLES_CLAIM_NAME, new String[]{AuthConstants.MANAGEMENT_CONSOLE_REFRESH_AUTHORITY})
//				  .sign(Algorithm.RSA256(publicKey, privateKey));
//	}
//
//	public String createRefreshToken(Customer customer) {
//		return JWT.create()
//				  .withIssuer(jwtProperties.getIssuer())
//				  .withSubject(String.valueOf(customer.getId()))
//				  .withIssuedAt(new Date())
//				  .withExpiresAt(getExpirationTime(Calendar.DAY_OF_MONTH, portalRefreshLifetime.value()))
//				  .withArrayClaim(ROLES_CLAIM_NAME, new String[]{AuthConstants.CUSTOMER_PORTAL_REFRESH_AUTHORITY})
//				  .sign(Algorithm.RSA256(publicKey, privateKey));
//	}
//
//	public String createRefreshTokenWithLoginChannel(Customer customer, String loginChannel) {
//		return JWT.create()
//				  .withIssuer(jwtProperties.getIssuer())
//				  .withSubject(String.valueOf(customer.getId()))
//				  .withIssuedAt(new Date())
//				  .withExpiresAt(getExpirationTime(Calendar.DAY_OF_MONTH, portalRefreshLifetime.value()))
//				  .withClaim(LOGIN_CHANNEL_CLAIM, loginChannel)
//				  .withArrayClaim(ROLES_CLAIM_NAME, new String[]{AuthConstants.CUSTOMER_PORTAL_REFRESH_AUTHORITY})
//				  .sign(Algorithm.RSA256(publicKey, privateKey));
//	}
//
//    public String createTemporaryToken(Customer customer) {
//        return JWT.create()
//                  .withIssuer(jwtProperties.getIssuer())
//                  .withSubject(String.valueOf(customer.getId()))
//                  .withIssuedAt(new Date())
//                  .withExpiresAt(getExpirationTime(Calendar.SECOND, portalTempLifetime.value()))
//                  .withArrayClaim(ROLES_CLAIM_NAME, new String[]{AuthConstants.CUSTOMER_PORTAL_AUTHORITY_TEMP})
//                  .sign(Algorithm.RSA256(publicKey, privateKey));
//    }
//
//    public String createConfirmationToken(long customerId, String customerEmail) {
//        return EncodingUtils.encodeBase64(JWT.create()
//                                             .withIssuer(jwtProperties.getIssuer())
//                                             .withSubject(String.valueOf(customerId))
//                                             .withClaim(EMAIL_CLAIM_NAME, customerEmail)
//                                             .withExpiresAt(getExpirationTime(Calendar.SECOND, confirmationTokenLifetime.value()))
//                                             .sign(Algorithm.RSA256(publicKey, privateKey)));
//    }
//
//    public DecodedJWT validateConfirmationTokenAndGetJwt(String token) throws LcmsException {
//        try {
//            token = EncodingUtils.decodeBase64(token);
//            JWTVerifier verifier = JWT.require(Algorithm.RSA256(publicKey, privateKey))
//                                      .withIssuer(jwtProperties.getIssuer())
//                                      .acceptExpiresAt(confirmationTokenLifetime.value())
//                                      .build();
//            return verifier.verify(token);
//        } catch (JWTVerificationException ex) {
//			throw new LcmsException(ExceptionKeys.AUTHENTICATION_CONFIRMATION_TOKEN_IS_INVALID);
//		}
//	}
//
//	public String systemUser() {
//		Jwt jwt = getJwt();
//		if (jwt == null || !isConsoleUser(jwt)) {
//			throw new IllegalStateException("AuthManager.systemUsername was called but system user is not logged in");
//		}
//		return jwt.getClaimAsString(USERNAME_CLAIM_NAME);
//	}
//
//	public boolean isAuthenticatedConsoleUser() {
//		Jwt jwt = getJwt();
//		return jwt != null && isConsoleUser(jwt);
//	}
//
//	public boolean isAuthenticatedPortalUser() {
//		Jwt jwt = getJwt();
//		return jwt != null && isPortalUser(jwt);
//	}
//
//	public boolean isAuthenticated() {
//		Jwt jwt = getJwt();
//		return jwt != null && (isConsoleUser(jwt) || isPortalUser(jwt));
//	}
//
//	public long systemUserId() {
//		Jwt jwt = getJwt();
//		if (jwt == null || !isConsoleUser(jwt)) {
//			throw new IllegalStateException("AuthManager.systemUsername was called but system user is not logged in");
//		}
//		return Long.parseLong(jwt.getSubject());
//	}
//
//	public long customerId() {
//		Jwt jwt = getJwt();
//		if (jwt == null || !isPortalUser(jwt)) {
//			throw new IllegalStateException("AuthManager.customerId was called but customers is not logged in");
//		}
//		return Long.parseLong(jwt.getSubject());
//	}
//
//	public boolean isUserInRole(String role) {
//		Jwt jwt = getJwt();
//		return getRoles(jwt).contains(role);
//	}
//
//	public String getLoginChannel() {
//		Jwt jwt = getJwt();
//		if (jwt == null || !isPortalUser(jwt)) {
//			throw new IllegalStateException("AuthManager.customerId was called but customers is not logged in");
//		}
//		return jwt.getClaimAsString(LOGIN_CHANNEL_CLAIM);
//	}
//
//
//	private Date getExpirationTime(int unit, int duration) {
//		Calendar calendar = Calendar.getInstance();
//		calendar.add(unit, duration);
//		return calendar.getTime();
//	}
//
//	public Jwt getJwt() {
//		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//		if (authentication != null) {
//			Object principal = authentication.getPrincipal();
//
//			if (principal instanceof Jwt) {
//				return (Jwt) principal;
//			} else {
//				return null;
//			}
//		} else {
//			return null;
//		}
//	}
//
//	public String getAuthenticatedUserName() {
//		Jwt jwt = getJwt();
//		if (jwt != null) {
//			return jwt.getClaimAsString(USERNAME_CLAIM_NAME);
//		}
//		return null;
//	}
//
//	public boolean isConsoleUser() {
//		Jwt jwt = getJwt();
//		if (jwt == null) {
//			throw new IllegalStateException("AuthManager.isConsoleUser was called but user is not logged in");
//		}
//		return isConsoleUser(jwt);
//	}
//
//	public List<Long> getCardTypeAccessPermissionIds() {
//		return getAccessPermissionIds(getJwt(), CARD_TYPE_ACCESS_CLAIM_NAME)
//			.stream()
//			.map(Long::valueOf)
//			.collect(Collectors.toList());
//	}
//
//    public List<Long> getCardTypeManagePermissionIds() {
//        return getAccessPermissionIds(getJwt(), CARD_TYPE_MANAGE_CLAIM_NAME)
//                .stream()
//                .map(Long::valueOf)
//                .collect(Collectors.toList());
//    }
//
//	public List<Long> getCompanyAccessPermissionIds() {
//		return getAccessPermissionIds(getJwt(), COMPANY_ACCESS_CLAIM_NAME)
//			.stream()
//			.map(Long::valueOf)
//			.collect(Collectors.toList());
//	}
//
//	private boolean isConsoleUser(Jwt jwt) {
//		return getRoles(jwt).contains(AuthConstants.MANAGEMENT_CONSOLE_AUTHORITY) || getRoles(jwt).contains(AuthConstants.MANAGEMENT_CONSOLE_REFRESH_AUTHORITY);
//	}
//
//	private boolean isPortalUser(Jwt jwt) {
//		List<String> roles = getRoles(jwt);
//		return roles.contains(AuthConstants.CUSTOMER_PORTAL_AUTHORITY_TEMP)
//			|| roles.contains(AuthConstants.CUSTOMER_PORTAL_AUTHORITY)
//			|| roles.contains(AuthConstants.CUSTOMER_PORTAL_REFRESH_AUTHORITY);
//	}
//
//	private List<String> getRoles(Jwt jwt) {
//		return Optional.ofNullable(jwt)
//					   .map(token -> token.getClaimAsStringList(ROLES_CLAIM_NAME))
//					   .orElseGet(List::of);
//	}
//
//	public List<String> getAccessPermissionIds(Jwt jwt, String permissionType) {
//		return Optional.ofNullable(jwt)
//					   .map(token -> token.getClaimAsStringList(permissionType))
//					   .orElseGet(List::of);
//	}
}
