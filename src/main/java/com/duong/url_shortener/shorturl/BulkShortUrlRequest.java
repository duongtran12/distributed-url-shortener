package com.duong.url_shortener.shorturl;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BulkShortUrlRequest(
		@NotEmpty @Size(max = 100) List<@NotNull @Positive Long> ids,
		@NotNull Action action) {

	public enum Action {
		ENABLE,
		DISABLE,
		DELETE
	}
}
