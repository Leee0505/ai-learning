package com.ticket.event;

import com.ticket.service.impl.NotificationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final NotificationServiceImpl notificationService;

    public KafkaEventConsumer(NotificationServiceImpl notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "ticket.created", groupId = "ticket-system")
    public void onTicketCreated(TicketCreatedEvent event) {
        log.info("AUDIT [ticket.created] ticketId={} title={} createdBy={}",
                event.getTicketId(), event.getTitle(), event.getCreatedBy());
        notificationService.handleTicketCreated(event);
    }

    @KafkaListener(topics = "ticket.assigned", groupId = "ticket-system")
    public void onTicketAssigned(TicketAssignedEvent event) {
        log.info("AUDIT [ticket.assigned] ticketId={} assignedTo={}",
                event.getTicketId(), event.getAssignedTo());
        notificationService.handleTicketAssigned(event);
    }

    @KafkaListener(topics = "ticket.overdue", groupId = "ticket-system")
    public void onTicketOverdue(TicketOverdueEvent event) {
        log.info("AUDIT [ticket.overdue] ticketId={} type={} overdueMinutes={}",
                event.getTicketId(), event.getType(), event.getOverdueMinutes());
        notificationService.handleTicketOverdue(event);
    }
}
