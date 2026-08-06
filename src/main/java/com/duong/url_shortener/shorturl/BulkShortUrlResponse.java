package com.duong.url_shortener.shorturl;

public record BulkShortUrlResponse(
		BulkShortUrlRequest.Action action,
		int affected) {
}
