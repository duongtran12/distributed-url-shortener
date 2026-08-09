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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected RefreshToken() {
	}

	private RefreshToken(User user, String tokenHash, Instant expiresAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	static RefreshToken create(User user, String tokenHash, Instant expiresAt) {
		return new RefreshToken(user, tokenHash, expiresAt);
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	void revoke(Instant now) {
		revokedAt = now;
	}

	boolean isUsableAt(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now) && user.isEnabled();
	}

	User getUser() {
		return user;
	}
}
