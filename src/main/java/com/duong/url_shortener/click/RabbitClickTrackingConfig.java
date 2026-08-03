package com.duong.url_shortener.click;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

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
		return QueueBuilder.durable(properties.queue())
				.deadLetterExchange(properties.deadLetterExchange())
				.deadLetterRoutingKey(properties.deadLetterRoutingKey())
				.build();
	}

	@Bean
	Binding clickBinding(
			@Qualifier("clickExchange") DirectExchange clickExchange,
			@Qualifier("clickQueue") Queue clickQueue,
			ClickTrackingProperties properties) {
		return BindingBuilder.bind(clickQueue)
				.to(clickExchange)
				.with(properties.routingKey());
	}

	@Bean
	DirectExchange clickDeadLetterExchange(ClickTrackingProperties properties) {
		return new DirectExchange(properties.deadLetterExchange(), true, false);
	}

	@Bean
	Queue clickDeadLetterQueue(ClickTrackingProperties properties) {
		return QueueBuilder.durable(properties.deadLetterQueue()).build();
	}

	@Bean
	Binding clickDeadLetterBinding(
			@Qualifier("clickDeadLetterExchange") DirectExchange clickDeadLetterExchange,
			@Qualifier("clickDeadLetterQueue") Queue clickDeadLetterQueue,
			ClickTrackingProperties properties) {
		return BindingBuilder.bind(clickDeadLetterQueue)
				.to(clickDeadLetterExchange)
				.with(properties.deadLetterRoutingKey());
	}
}
