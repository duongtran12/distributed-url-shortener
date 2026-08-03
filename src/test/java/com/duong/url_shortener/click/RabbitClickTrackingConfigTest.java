package com.duong.url_shortener.click;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

class RabbitClickTrackingConfigTest {

	private final ClickTrackingProperties properties = new ClickTrackingProperties(
			true,
			"clicks.exchange",
			"clicks.queue",
			"click.recorded",
			"clicks.dlx",
			"clicks.dead",
			"click.dead");
	private final RabbitClickTrackingConfig config = new RabbitClickTrackingConfig();

	@Test
	void shouldConfigureMainQueueWithDeadLetterRouting() {
		Queue queue = config.clickQueue(properties);

		assertEquals("clicks.queue", queue.getName());
		assertEquals("clicks.dlx", queue.getArguments().get("x-dead-letter-exchange"));
		assertEquals("click.dead", queue.getArguments().get("x-dead-letter-routing-key"));
	}

	@Test
	void shouldBindDeadLetterQueue() {
		DirectExchange exchange = config.clickDeadLetterExchange(properties);
		Queue queue = config.clickDeadLetterQueue(properties);
		Binding binding = config.clickDeadLetterBinding(exchange, queue, properties);

		assertEquals("clicks.dlx", exchange.getName());
		assertEquals("clicks.dead", queue.getName());
		assertEquals("click.dead", binding.getRoutingKey());
	}
}
