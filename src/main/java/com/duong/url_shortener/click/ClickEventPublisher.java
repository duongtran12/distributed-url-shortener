package com.duong.url_shortener.click;

import java.time.Instant;

public interface ClickEventPublisher {

	void publish(String shortCode, Instant clickedAt, String userAgent, String referrer);
}
