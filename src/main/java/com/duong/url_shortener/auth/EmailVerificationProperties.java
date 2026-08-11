package com.duong.url_shortener.auth;

import java.time.Duration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.email-verification")
public record EmailVerificationProperties(
		@NotNull Duration expiration,
		@NotBlank String frontendUrl,
		@NotBlank @Email String fromAddress) {
}
