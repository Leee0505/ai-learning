package com.ticket.event;

/**
 * Thin wrapper around KafkaTemplate for typed event publishing.
 */
public interface EventPublisher {

    /** Publish a ticket-created event to topic "ticket.created". */
    void publishTicketCreated(TicketCreatedEvent event);

    /** Publish a ticket-assigned event to topic "ticket.assigned". */
    void publishTicketAssigned(TicketAssignedEvent event);
}
