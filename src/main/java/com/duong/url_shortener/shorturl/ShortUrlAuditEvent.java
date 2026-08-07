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
import jakarta.persistence.Table;

@Entity
@Table(name = "short_url_audit_events")
public class ShortUrlAuditEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User owner;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "short_url_id")
	private ShortUrl shortUrl;

	@Column(name = "short_code", nullable = false, length = 32)
	private String shortCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ShortUrlAuditAction action;

	@Column(nullable = false, length = 255)
	private String details;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ShortUrlAuditEvent() {
	}

	private ShortUrlAuditEvent(User owner, ShortUrl shortUrl, ShortUrlAuditAction action, String details) {
		this.owner = owner;
		this.shortUrl = shortUrl;
		this.shortCode = shortUrl.getShortCode();
		this.action = action;
		this.details = details;
	}

	private ShortUrlAuditEvent(User owner, String shortCode, ShortUrlAuditAction action, String details) {
		this.owner = owner;
		this.shortCode = shortCode;
		this.action = action;
		this.details = details;
	}

	static ShortUrlAuditEvent create(User owner, ShortUrl shortUrl, ShortUrlAuditAction action, String details) {
		return new ShortUrlAuditEvent(owner, shortUrl, action, details);
	}

	static ShortUrlAuditEvent createDeletedSnapshot(User owner, String shortCode, String details) {
		return new ShortUrlAuditEvent(owner, shortCode, ShortUrlAuditAction.DELETED, details);
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public Long getId() { return id; }
	public ShortUrl getShortUrl() { return shortUrl; }
	public String getShortCode() { return shortCode; }
	public ShortUrlAuditAction getAction() { return action; }
	public String getDetails() { return details; }
	public Instant getCreatedAt() { return createdAt; }
}
