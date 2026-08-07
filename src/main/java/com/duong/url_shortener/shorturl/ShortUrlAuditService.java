package com.duong.url_shortener.shorturl;

import com.duong.url_shortener.common.exception.ApiException;
import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortUrlAuditService {

	private final ShortUrlAuditRepository auditRepository;
	private final UserRepository userRepository;

	public ShortUrlAuditService(ShortUrlAuditRepository auditRepository, UserRepository userRepository) {
		this.auditRepository = auditRepository;
		this.userRepository = userRepository;
	}

	void record(User owner, ShortUrl shortUrl, ShortUrlAuditAction action, String details) {
		auditRepository.saveAndFlush(ShortUrlAuditEvent.create(owner, shortUrl, action, details));
	}

	void recordDeleted(User owner, String shortCode, String details) {
		auditRepository.saveAndFlush(ShortUrlAuditEvent.createDeletedSnapshot(owner, shortCode, details));
	}

	@Transactional(readOnly = true)
	public ShortUrlAuditPageResponse findAll(Long userId, ShortUrlAuditAction action, int page, int size) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(
						HttpStatus.UNAUTHORIZED,
						"INVALID_ACCESS_TOKEN",
						"The access token no longer belongs to an existing user"));
		if (!user.isEnabled()) {
			throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "The user account is disabled");
		}
		PageRequest pageable = PageRequest.of(page, size, Sort.by(
				Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
		return ShortUrlAuditPageResponse.from(action == null
				? auditRepository.findAllByOwnerId(userId, pageable)
				: auditRepository.findAllByOwnerIdAndAction(userId, action, pageable));
	}
}
