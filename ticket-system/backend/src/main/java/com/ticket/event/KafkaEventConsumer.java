package com.ticket.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes ticket events for audit logging and notification.
 * For MVP, audit events are logged via SLF4J (ready for ELK).
 * Email notification is a placeholder for future async sending.
 */
@Component
public class KafkaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

    @KafkaListener(topics = "ticket.created", groupId = "ticket-system")
    public void onTicketCreated(TicketCreatedEvent event) {
        // MVP: structured log for audit trail (ELK-ready)
        log.info("AUDIT [ticket.created] ticketId={} title={} priority={} category={} createdBy={}",
                event.getTicketId(), event.getTitle(), event.getPriority(),
                event.getCategory(), event.getCreatedBy());
    }

    @KafkaListener(topics = "ticket.assigned", groupId = "ticket-system")
    public void onTicketAssigned(TicketAssignedEvent event) {
        log.info("AUDIT [ticket.assigned] ticketId={} assignedTo={} assignedBy={}",
                event.getTicketId(), event.getAssignedTo(), event.getAssignedBy());
    }
}
