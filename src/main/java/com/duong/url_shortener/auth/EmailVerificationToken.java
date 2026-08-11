package com.duong.url_shortener.auth;

import java.time.Instant;

import com.duong.url_shortener.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected EmailVerificationToken() {
	}

	private EmailVerificationToken(User user, String tokenHash, Instant expiresAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	static EmailVerificationToken create(User user, String tokenHash, Instant expiresAt) {
		return new EmailVerificationToken(user, tokenHash, expiresAt);
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	boolean isUsableAt(Instant now) {
		return !user.isEnabled() && expiresAt.isAfter(now);
	}

	User getUser() {
		return user;
	}
}
