package com.duong.url_shortener.auth;

import java.time.Instant;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<PasswordResetToken> findByTokenHash(String tokenHash);

	@Modifying
	@Query("UPDATE PasswordResetToken token SET token.usedAt = :now "
			+ "WHERE token.user.id = :userId AND token.usedAt IS NULL")
	int consumeAllForUser(Long userId, Instant now);
}
