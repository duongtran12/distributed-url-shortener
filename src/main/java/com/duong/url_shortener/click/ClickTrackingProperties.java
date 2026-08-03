package com.duong.url_shortener.click;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.click-tracking")
public record ClickTrackingProperties(
		boolean enabled,
		@NotBlank String exchange,
		@NotBlank String queue,
		@NotBlank String routingKey,
		@NotBlank String deadLetterExchange,
		@NotBlank String deadLetterQueue,
		@NotBlank String deadLetterRoutingKey) {
}
