package com.duong.url_shortener.shorturl;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BulkShortUrlRequest(
		@NotEmpty @Size(max = 100) List<@NotNull @Positive Long> ids,
		@NotNull Action action,
		@Size(max = 32) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]*") String tag) {

	public enum Action {
		ENABLE,
		DISABLE,
		DELETE,
		SET_TAG,
		CLEAR_TAG
	}
}
