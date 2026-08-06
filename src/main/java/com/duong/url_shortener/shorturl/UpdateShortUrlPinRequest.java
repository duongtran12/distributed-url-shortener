package com.duong.url_shortener.shorturl;

import jakarta.validation.constraints.NotNull;

public record UpdateShortUrlPinRequest(@NotNull Boolean pinned) {
}
