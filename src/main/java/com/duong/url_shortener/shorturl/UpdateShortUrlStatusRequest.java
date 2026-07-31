package com.duong.url_shortener.shorturl;

import jakarta.validation.constraints.NotNull;

public record UpdateShortUrlStatusRequest(
		@NotNull OwnerShortUrlStatus status) {

	public enum OwnerShortUrlStatus {
		ACTIVE,
		DISABLED
	}
}
