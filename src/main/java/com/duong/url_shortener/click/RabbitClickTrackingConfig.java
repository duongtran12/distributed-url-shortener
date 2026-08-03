package com.duong.url_shortener.click;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
		prefix = "app.click-tracking",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true)
public class RabbitClickTrackingConfig {

	@Bean
	DirectExchange clickExchange(ClickTrackingProperties properties) {
		return new DirectExchange(properties.exchange(), true, false);
	}

	@Bean
	Queue clickQueue(ClickTrackingProperties properties) {
		return new Queue(properties.queue(), true);
	}

	@Bean
	Binding clickBinding(
			DirectExchange clickExchange,
			Queue clickQueue,
			ClickTrackingProperties properties) {
		return BindingBuilder.bind(clickQueue)
				.to(clickExchange)
				.with(properties.routingKey());
	}
}
