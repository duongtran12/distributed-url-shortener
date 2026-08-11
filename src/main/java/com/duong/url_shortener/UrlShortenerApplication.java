package com.duong.url_shortener;

import com.duong.url_shortener.click.ClickTrackingProperties;
import com.duong.url_shortener.click.ClickEventRetentionProperties;
import com.duong.url_shortener.auth.RefreshTokenProperties;
import com.duong.url_shortener.auth.RefreshTokenCleanupProperties;
import com.duong.url_shortener.auth.PasswordResetProperties;
import com.duong.url_shortener.auth.PasswordResetCleanupProperties;
import com.duong.url_shortener.auth.EmailVerificationProperties;
import com.duong.url_shortener.click.VisitorFingerprintProperties;
import com.duong.url_shortener.ratelimit.RateLimitProperties;
import com.duong.url_shortener.shorturl.ShortUrlProperties;
import com.duong.url_shortener.shorturl.RedirectCacheProperties;
import com.duong.url_shortener.shorturl.AuditRetentionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
		ShortUrlProperties.class,
		RedirectCacheProperties.class,
		ClickTrackingProperties.class,
		ClickEventRetentionProperties.class,
		VisitorFingerprintProperties.class,
		RateLimitProperties.class,
		AuditRetentionProperties.class,
		RefreshTokenProperties.class,
		RefreshTokenCleanupProperties.class,
		PasswordResetProperties.class,
		PasswordResetCleanupProperties.class,
		EmailVerificationProperties.class
})
public class UrlShortenerApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrlShortenerApplication.class, args);
	}

}
