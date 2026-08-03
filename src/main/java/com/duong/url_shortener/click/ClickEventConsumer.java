package com.duong.url_shortener.click;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
		prefix = "app.click-tracking",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true)
public class ClickEventConsumer {

	private final ObjectMapper objectMapper;
	private final ClickEventStore clickEventStore;

	public ClickEventConsumer(ObjectMapper objectMapper, ClickEventStore clickEventStore) {
		this.objectMapper = objectMapper;
		this.clickEventStore = clickEventStore;
	}

	@RabbitListener(queues = "${app.click-tracking.queue}")
	public void consume(String payload) {
		try {
			clickEventStore.record(objectMapper.readValue(payload, ClickEvent.class));
		} catch (JacksonException exception) {
			throw new AmqpRejectAndDontRequeueException("Malformed click event", exception);
		}
	}
}
