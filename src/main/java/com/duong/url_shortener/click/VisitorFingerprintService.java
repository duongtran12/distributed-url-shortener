package com.duong.url_shortener.click;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class VisitorFingerprintService {

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	private final SecretKeySpec secretKey;

	public VisitorFingerprintService(VisitorFingerprintProperties properties) {
		this.secretKey = new SecretKeySpec(
				properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
	}

	public String hash(String clientAddress) {
		if (clientAddress == null || clientAddress.isBlank()) {
			return null;
		}

		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(secretKey);
			return HexFormat.of().formatHex(
					mac.doFinal(clientAddress.strip().getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Unable to create visitor fingerprint", exception);
		}
	}
}
