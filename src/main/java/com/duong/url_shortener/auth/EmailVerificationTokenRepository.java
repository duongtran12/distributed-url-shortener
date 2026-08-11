package com.duong.url_shortener.auth;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

	@Modifying
	@Query("DELETE FROM EmailVerificationToken token WHERE token.user.id = :userId")
	int deleteByUserId(@Param("userId") Long userId);
}
