package com.duong.url_shortener.shorturl;

import org.springframework.data.domain.Sort;

public enum ShortUrlSort {
	NEWEST,
	OLDEST,
	MOST_CLICKED;

	Sort toSort() {
		return switch (this) {
			case NEWEST -> Sort.by(
					Sort.Order.desc("createdAt"),
					Sort.Order.desc("id"));
			case OLDEST -> Sort.by(
					Sort.Order.asc("createdAt"),
					Sort.Order.asc("id"));
			case MOST_CLICKED -> Sort.by(
					Sort.Order.desc("clickCount"),
					Sort.Order.desc("createdAt"),
					Sort.Order.desc("id"));
		};
	}
}
