package com.duong.url_shortener.auth;

import java.util.Optional;
import java.time.Instant;
import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	Optional<RefreshToken> findByIdAndUserId(Long id, Long userId);

	List<RefreshToken> findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
			Long userId,
			Instant now);

	@Modifying
	@Query("UPDATE RefreshToken token SET token.revokedAt = :now "
			+ "WHERE token.user.id = :userId AND token.revokedAt IS NULL")
	int revokeAllByUserId(Long userId, Instant now);

	@Modifying
	@Query("UPDATE RefreshToken token SET token.revokedAt = :now "
			+ "WHERE token.user.id = :userId AND token.id <> :currentId "
			+ "AND token.revokedAt IS NULL AND token.expiresAt > :now")
	int revokeAllOtherSessions(Long userId, Long currentId, Instant now);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE refresh_tokens
			SET revoked_at = :now
			WHERE id IN (
			    SELECT id
			    FROM refresh_tokens
			    WHERE user_id = :userId
			      AND revoked_at IS NULL
			      AND expires_at > :now
			    ORDER BY CASE WHEN id = :currentId THEN 1 ELSE 0 END DESC,
			             created_at DESC,
			             id DESC
			    OFFSET :maxActiveSessions
			)
			""", nativeQuery = true)
	int revokeSessionsExceedingLimit(
			Long userId,
			Long currentId,
			Instant now,
			int maxActiveSessions);
}
