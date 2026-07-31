package com.duong.url_shortener.shorturl;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

	Optional<ShortUrl> findByShortCode(String shortCode);

	Optional<ShortUrl> findByIdAndOwnerId(Long id, Long ownerId);

	Page<ShortUrl> findAllByOwnerId(Long ownerId, Pageable pageable);

	boolean existsByShortCode(String shortCode);
}
