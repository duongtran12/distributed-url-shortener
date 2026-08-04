package com.duong.url_shortener;

import com.duong.url_shortener.click.ClickTrackingProperties;
import com.duong.url_shortener.click.VisitorFingerprintProperties;
import com.duong.url_shortener.ratelimit.RateLimitProperties;
import com.duong.url_shortener.shorturl.ShortUrlProperties;
import com.duong.url_shortener.shorturl.RedirectCacheProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
		ShortUrlProperties.class,
		RedirectCacheProperties.class,
		ClickTrackingProperties.class,
		VisitorFingerprintProperties.class,
		RateLimitProperties.class
})
public class UrlShortenerApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrlShortenerApplication.class, args);
	}

}
