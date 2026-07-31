package com.duong.url_shortener.shorturl;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

	Optional<ShortUrl> findByShortCode(String shortCode);

	boolean existsByShortCode(String shortCode);
}
