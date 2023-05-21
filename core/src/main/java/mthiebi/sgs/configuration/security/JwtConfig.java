package mthiebi.sgs.configuration.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
//import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
//import org.springframework.security.oauth2.jwt.JwtDecoder;
//import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
//import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
//import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

@Slf4j
@Configuration
public class JwtConfig {

//	@Autowired
//	private JwtProperties jwtProperties;
//
//	@Bean
//	public KeyStore keyStore() {
//		try {
//			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//			InputStream resourceAsStream = new ClassPathResource(jwtProperties.getKeyStorePath()).getInputStream();
//			keyStore.load(resourceAsStream, jwtProperties.getKeyStorePassword().toCharArray());
//			return keyStore;
//		} catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
//			log.error("Unable to load keystore {}", jwtProperties.getKeyStorePath());
//		}
//
//		throw new IllegalArgumentException("Unable to load keystore");
//	}
//
//	@Bean
//	public RSAPrivateKey jwtSigningKey(KeyStore keyStore) {
//		try {
//			Key key = keyStore.getKey(jwtProperties.getKeyAlias(), jwtProperties.getPrivateKeyPassphrase().toCharArray());
//			if (key instanceof RSAPrivateKey) {
//				return (RSAPrivateKey) key;
//			}
//		} catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
//			log.error("Unable to load RSA private key {}", jwtProperties.getKeyAlias());
//		}
//
//		throw new IllegalArgumentException("Unable to load RSA private key");
//	}
//
//	@Bean
//	public RSAPublicKey jwtValidationKey(KeyStore keyStore) {
//		try {
//			Certificate certificate = keyStore.getCertificate(jwtProperties.getKeyAlias());
//			PublicKey publicKey = certificate.getPublicKey();
//
//			if (publicKey instanceof RSAPublicKey) {
//				return (RSAPublicKey) publicKey;
//			}
//		} catch (KeyStoreException e) {
//			log.error("Unable to load RSA public key {}", jwtProperties.getKeyAlias());
//		}
//
//		throw new IllegalArgumentException("Unable to load RSA public key");
//	}
//
//	@Bean
//	public JwtDecoder jwtDecoder(RSAPublicKey rsaPublicKey) {
//		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsaPublicKey).build();
//		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(Arrays.asList(
//			new JwtTimestampValidator(),
//			new JwtIssuerValidator(jwtProperties.getIssuer())
//		)));
//		return decoder;
//	}

}
