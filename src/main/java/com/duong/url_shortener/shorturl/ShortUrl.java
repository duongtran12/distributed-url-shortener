package com.duong.url_shortener.shorturl;

import java.time.Instant;

import com.duong.url_shortener.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "short_urls")
public class ShortUrl {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User owner;

	@Column(name = "short_code", nullable = false, unique = true, length = 32)
	private String shortCode;

	@Column(name = "original_url", nullable = false, length = 2048)
	private String originalUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ShortUrlStatus status;

	@Column(name = "custom_alias", nullable = false)
	private boolean customAlias;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ShortUrl() {
	}

	private ShortUrl(
			User owner,
			String shortCode,
			String originalUrl,
			boolean customAlias,
			Instant expiresAt) {
		this.owner = owner;
		this.shortCode = shortCode;
		this.originalUrl = originalUrl;
		this.status = ShortUrlStatus.ACTIVE;
		this.customAlias = customAlias;
		this.expiresAt = expiresAt;
	}

	public static ShortUrl create(
			User owner,
			String shortCode,
			String originalUrl,
			boolean customAlias,
			Instant expiresAt) {
		return new ShortUrl(owner, shortCode, originalUrl, customAlias, expiresAt);
	}

	public boolean isExpiredAt(Instant instant) {
		return expiresAt != null && !expiresAt.isAfter(instant);
	}

	public boolean isRedirectableAt(Instant instant) {
		return status == ShortUrlStatus.ACTIVE && !isExpiredAt(instant);
	}

	public void disable() {
		if (status != ShortUrlStatus.BLOCKED) {
			status = ShortUrlStatus.DISABLED;
		}
	}

	public void enable() {
		if (status != ShortUrlStatus.BLOCKED) {
			status = ShortUrlStatus.ACTIVE;
		}
	}

	public void block() {
		status = ShortUrlStatus.BLOCKED;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public User getOwner() {
		return owner;
	}

	public String getShortCode() {
		return shortCode;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public ShortUrlStatus getStatus() {
		return status;
	}

	public boolean isCustomAlias() {
		return customAlias;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public long getVersion() {
		return version;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
