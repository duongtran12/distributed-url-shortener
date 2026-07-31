package com.duong.url_shortener.shorturl;

import java.util.List;

import org.springframework.data.domain.Page;

public record ShortUrlPageResponse(
		List<ShortUrlResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	static ShortUrlPageResponse from(Page<ShortUrl> shortUrls, String baseUrl) {
		return new ShortUrlPageResponse(
				shortUrls.getContent().stream()
						.map(shortUrl -> ShortUrlResponse.from(shortUrl, baseUrl))
						.toList(),
				shortUrls.getNumber(),
				shortUrls.getSize(),
				shortUrls.getTotalElements(),
				shortUrls.getTotalPages());
	}
}
