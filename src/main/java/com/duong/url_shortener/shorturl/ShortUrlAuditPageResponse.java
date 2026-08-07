package com.duong.url_shortener.shorturl;

import java.util.List;

import org.springframework.data.domain.Page;

public record ShortUrlAuditPageResponse(
		List<ShortUrlAuditResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	static ShortUrlAuditPageResponse from(Page<ShortUrlAuditEvent> events) {
		return new ShortUrlAuditPageResponse(
				events.getContent().stream().map(ShortUrlAuditResponse::from).toList(),
				events.getNumber(), events.getSize(), events.getTotalElements(), events.getTotalPages());
	}
}
