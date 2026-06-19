package com.ticket.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.entity.SlaConfig;
import com.ticket.entity.Ticket;
import com.ticket.event.EventPublisher;
import com.ticket.event.TicketOverdueEvent;
import com.ticket.mapper.SlaConfigMapper;
import com.ticket.mapper.TicketMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SlaCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlaCheckScheduler.class);

    private final TicketMapper ticketMapper;
    private final SlaConfigMapper slaConfigMapper;
    private final EventPublisher eventPublisher;

    public SlaCheckScheduler(TicketMapper ticketMapper, SlaConfigMapper slaConfigMapper,
                             EventPublisher eventPublisher) {
        this.ticketMapper = ticketMapper;
        this.slaConfigMapper = slaConfigMapper;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = 300_000) // Every 5 minutes
    public void detectOverdueTickets() {
        long now = System.currentTimeMillis();
        List<SlaConfig> slas = slaConfigMapper.selectList(new LambdaQueryWrapper<>());
        if (slas.isEmpty()) {
            log.debug("No SLA configs found, skipping overdue check");
            return;
        }

        int detected = 0;

        // 1. Unassigned OPEN tickets past response SLA
        List<Ticket> unassigned = ticketMapper.selectList(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getStatus, "OPEN")
                .isNull(Ticket::getAssignedTo));

        for (Ticket ticket : unassigned) {
            SlaConfig sla = findSla(slas, ticket.getPriority());
            if (sla == null) continue;
            long deadline = ticket.getCreatedDate() + (long) sla.getResponseMinutes() * 60_000;
            if (now > deadline) {
                int overdueMin = (int) ((now - deadline) / 60_000);
                eventPublisher.publishTicketOverdue(
                        new TicketOverdueEvent(ticket.getId(), "UNASSIGNED_OVERDUE", overdueMin, null, ticket.getPriority()));
                detected++;
            }
        }

        // 2. Assigned OPEN/IN_PROGRESS tickets past resolution SLA
        List<Ticket> assigned = ticketMapper.selectList(new LambdaQueryWrapper<Ticket>()
                .in(Ticket::getStatus, "OPEN", "IN_PROGRESS")
                .isNotNull(Ticket::getAssignedTo));

        for (Ticket ticket : assigned) {
            SlaConfig sla = findSla(slas, ticket.getPriority());
            if (sla == null) continue;
            long deadline = ticket.getCreatedDate() + (long) sla.getResolutionMinutes() * 60_000;
            if (now > deadline) {
                int overdueMin = (int) ((now - deadline) / 60_000);
                eventPublisher.publishTicketOverdue(
                        new TicketOverdueEvent(ticket.getId(), "RESOLUTION_OVERDUE", overdueMin, ticket.getAssignedTo(), ticket.getPriority()));
                detected++;
            }
        }

        if (detected > 0) {
            log.info("Overdue check complete: {} overdue tickets detected", detected);
        }
    }

    private SlaConfig findSla(List<SlaConfig> slas, String priority) {
        return slas.stream()
                .filter(s -> s.getPriority().equalsIgnoreCase(priority))
                .findFirst()
                .orElse(null);
    }
}
