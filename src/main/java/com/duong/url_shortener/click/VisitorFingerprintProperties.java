package com.duong.url_shortener.click;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.visitor-fingerprint")
public record VisitorFingerprintProperties(
		@NotBlank @Size(min = 32) String secret) {
}
