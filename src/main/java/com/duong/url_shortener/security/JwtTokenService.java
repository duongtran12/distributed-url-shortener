package com.duong.url_shortener.security;

import java.time.Instant;
import java.util.List;

import com.duong.url_shortener.user.User;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;

	public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
		this.jwtEncoder = jwtEncoder;
		this.jwtProperties = jwtProperties;
	}

	public String createAccessToken(User user) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenExpiration());

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("url-shortener")
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(user.getEmail())
				.claim("uid", user.getId())
				.claim("roles", List.of(user.getRole().name()))
				.build();

		return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}

	public long accessTokenExpiresInSeconds() {
		return jwtProperties.accessTokenExpiration().toSeconds();
	}
}
