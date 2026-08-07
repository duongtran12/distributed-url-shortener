package com.duong.url_shortener.shorturl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortUrlAuditRepository extends JpaRepository<ShortUrlAuditEvent, Long> {
	Page<ShortUrlAuditEvent> findAllByOwnerId(Long ownerId, Pageable pageable);

	Page<ShortUrlAuditEvent> findAllByOwnerIdAndAction(
			Long ownerId,
			ShortUrlAuditAction action,
			Pageable pageable);
}
