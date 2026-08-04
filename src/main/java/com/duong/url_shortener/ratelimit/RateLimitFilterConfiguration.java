package com.duong.url_shortener.ratelimit;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitFilterConfiguration {

	@Bean
	FilterRegistrationBean<RateLimitFilter> disableRateLimitFilterAutoRegistration(
			RateLimitFilter filter) {
		FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}
}
