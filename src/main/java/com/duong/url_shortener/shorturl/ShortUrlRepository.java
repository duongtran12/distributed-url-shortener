package com.duong.url_shortener.shorturl;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

	Optional<ShortUrl> findByShortCode(String shortCode);

	Optional<ShortUrl> findByIdAndOwnerId(Long id, Long ownerId);

	Page<ShortUrl> findAllByOwnerId(Long ownerId, Pageable pageable);

	boolean existsByShortCode(String shortCode);

	@Modifying(clearAutomatically = true)
	@Query("""
			update ShortUrl shortUrl
			set shortUrl.clickCount = shortUrl.clickCount + 1
			where shortUrl.shortCode = :shortCode
			""")
	int incrementClickCount(@Param("shortCode") String shortCode);
}
