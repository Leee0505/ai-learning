package com.ticket.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    public static final String TOPIC_TICKET_CREATED  = "ticket.created";
    public static final String TOPIC_TICKET_ASSIGNED  = "ticket.assigned";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishTicketCreated(TicketCreatedEvent event) {
        send(TOPIC_TICKET_CREATED, String.valueOf(event.getTicketId()), event);
    }

    @Override
    public void publishTicketAssigned(TicketAssignedEvent event) {
        send(TOPIC_TICKET_ASSIGNED, String.valueOf(event.getTicketId()), event);
    }

    private void send(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka send failed: topic={} key={}", topic, key, ex);
                    } else {
                        log.debug("Kafka sent: topic={} key={} offset={}",
                                topic, key, result.getRecordMetadata().offset());
                    }
                });
    }
}
