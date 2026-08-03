package com.duong.url_shortener.click;

import java.time.Instant;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
		prefix = "app.click-tracking",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true)
public class RabbitClickEventPublisher implements ClickEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(RabbitClickEventPublisher.class);

	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;
	private final ClickTrackingProperties properties;

	public RabbitClickEventPublisher(
			RabbitTemplate rabbitTemplate,
			ObjectMapper objectMapper,
			ClickTrackingProperties properties) {
		this.rabbitTemplate = rabbitTemplate;
		this.objectMapper = objectMapper;
		this.properties = properties;
	}

	@Override
	public void publish(String shortCode, Instant clickedAt, String userAgent, String referrer) {
		try {
			String payload = objectMapper.writeValueAsString(
					ClickEvent.create(shortCode, clickedAt, userAgent, referrer));
			rabbitTemplate.convertAndSend(
					properties.exchange(),
					properties.routingKey(),
					payload);
		} catch (JacksonException | AmqpException exception) {
			log.warn("Click event publication failed; redirect will continue", exception);
		}
	}
}
