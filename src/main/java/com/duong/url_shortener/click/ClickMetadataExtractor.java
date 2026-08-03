package com.duong.url_shortener.click;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class ClickMetadataExtractor {

	private static final int MAX_USER_AGENT_LENGTH = 512;

	public ClickMetadata extract(ClickEvent event) {
		String userAgent = normalizeUserAgent(event.userAgent());
		String normalized = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
		return new ClickMetadata(
				userAgent,
				normalizeReferrer(event.referrer()),
				detectBrowser(normalized),
				detectOperatingSystem(normalized),
				detectDeviceType(normalized));
	}

	private String normalizeUserAgent(String userAgent) {
		if (userAgent == null || userAgent.isBlank()) {
			return null;
		}
		String normalized = userAgent.strip();
		return normalized.length() <= MAX_USER_AGENT_LENGTH
				? normalized
				: normalized.substring(0, MAX_USER_AGENT_LENGTH);
	}

	private String normalizeReferrer(String referrer) {
		if (referrer == null || referrer.isBlank()) {
			return "direct";
		}
		try {
			URI uri = new URI(referrer.strip());
			String host = uri.getHost();
			return host == null || host.isBlank()
					? "unknown"
					: host.toLowerCase(Locale.ROOT);
		} catch (URISyntaxException exception) {
			return "unknown";
		}
	}

	private String detectBrowser(String userAgent) {
		if (containsAny(userAgent, "edg/", "edge/")) {
			return "Edge";
		}
		if (containsAny(userAgent, "opr/", "opera")) {
			return "Opera";
		}
		if (containsAny(userAgent, "chrome/", "crios/")) {
			return "Chrome";
		}
		if (containsAny(userAgent, "firefox/", "fxios/")) {
			return "Firefox";
		}
		if (userAgent.contains("safari/") && !userAgent.contains("android")) {
			return "Safari";
		}
		return "Other";
	}

	private String detectOperatingSystem(String userAgent) {
		if (userAgent.contains("android")) {
			return "Android";
		}
		if (containsAny(userAgent, "iphone", "ipad", "ios")) {
			return "iOS";
		}
		if (userAgent.contains("windows")) {
			return "Windows";
		}
		if (containsAny(userAgent, "macintosh", "mac os")) {
			return "macOS";
		}
		if (userAgent.contains("linux")) {
			return "Linux";
		}
		return "Other";
	}

	private DeviceType detectDeviceType(String userAgent) {
		if (containsAny(userAgent, "bot", "crawler", "spider", "slurp")) {
			return DeviceType.BOT;
		}
		if (containsAny(userAgent, "ipad", "tablet")) {
			return DeviceType.TABLET;
		}
		if (containsAny(userAgent, "mobile", "iphone", "android")) {
			return DeviceType.MOBILE;
		}
		return userAgent.isBlank() ? DeviceType.UNKNOWN : DeviceType.DESKTOP;
	}

	private boolean containsAny(String value, String... candidates) {
		for (String candidate : candidates) {
			if (value.contains(candidate)) {
				return true;
			}
		}
		return false;
	}
}
