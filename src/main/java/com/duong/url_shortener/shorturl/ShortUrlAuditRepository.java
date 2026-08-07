package com.duong.url_shortener.shorturl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface ShortUrlAuditRepository extends JpaRepository<ShortUrlAuditEvent, Long> {
	Page<ShortUrlAuditEvent> findAllByOwnerId(Long ownerId, Pageable pageable);

	Page<ShortUrlAuditEvent> findAllByOwnerIdAndAction(
			Long ownerId,
			ShortUrlAuditAction action,
			Pageable pageable);

	@Modifying
	@Transactional
	@Query(value = """
			DELETE FROM short_url_audit_events
			WHERE id IN (
				SELECT id FROM short_url_audit_events
				WHERE created_at < :cutoff
				ORDER BY created_at, id
				LIMIT :batchSize
			)
			""", nativeQuery = true)
	int deleteOldestBatch(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);
}
