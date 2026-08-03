package com.duong.url_shortener.click;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitClickEventPublisherTest {

	private final ClickTrackingProperties properties = new ClickTrackingProperties(
			true,
			"clicks.exchange",
			"clicks.queue",
			"click.recorded",
			"clicks.dlx",
			"clicks.dead",
			"click.dead");

	@Test
	void shouldPublishJsonClickEvent() {
		RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
		RabbitClickEventPublisher publisher = new RabbitClickEventPublisher(
				rabbitTemplate,
				objectMapper(),
				properties);

		publisher.publish("abc1234", Instant.parse("2030-01-01T00:00:00Z"));

		verify(rabbitTemplate).convertAndSend(
				eq("clicks.exchange"),
				eq("click.recorded"),
				startsWith("{\"eventId\":"));
	}

	@Test
	void shouldNotBreakRedirectWhenRabbitMqIsUnavailable() {
		RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
		doThrow(new AmqpConnectException(new RuntimeException("RabbitMQ unavailable")))
				.when(rabbitTemplate)
				.convertAndSend(eq("clicks.exchange"), eq("click.recorded"), startsWith("{"));
		RabbitClickEventPublisher publisher = new RabbitClickEventPublisher(
				rabbitTemplate,
				objectMapper(),
				properties);

		assertDoesNotThrow(() -> publisher.publish("abc1234", Instant.now()));
	}

	private ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
