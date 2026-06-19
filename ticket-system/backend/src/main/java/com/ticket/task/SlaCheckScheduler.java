package com.ticket.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.common.constant.BusinessConstants;
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
import java.util.Map;
import java.util.stream.Collectors;

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

        Map<String, SlaConfig> slaByPriority = slas.stream()
                .collect(Collectors.toMap(SlaConfig::getPriority, s -> s));

        int detected = 0;

        // Query: all non-closed tickets (status-filtered at DB level)
        List<Ticket> active = ticketMapper.selectList(new LambdaQueryWrapper<Ticket>()
                .in(Ticket::getStatus, "OPEN", "IN_PROGRESS"));

        for (Ticket ticket : active) {
            SlaConfig sla = slaByPriority.get(ticket.getPriority());
            if (sla == null) continue;

            if (ticket.getAssignedTo() == null) {
                // Unassigned → check response SLA
                long deadline = ticket.getCreatedDate() + (long) sla.getResponseMinutes() * BusinessConstants.MILLIS_PER_MINUTE;
                if (now > deadline) {
                    int overdueMin = (int) ((now - deadline) / BusinessConstants.MILLIS_PER_MINUTE);
                    eventPublisher.publishTicketOverdue(
                            new TicketOverdueEvent(ticket.getId(), "UNASSIGNED_OVERDUE", overdueMin, null, ticket.getPriority()));
                    detected++;
                }
            } else {
                // Assigned → check resolution SLA
                long deadline = ticket.getCreatedDate() + (long) sla.getResolutionMinutes() * BusinessConstants.MILLIS_PER_MINUTE;
                if (now > deadline) {
                    int overdueMin = (int) ((now - deadline) / BusinessConstants.MILLIS_PER_MINUTE);
                    eventPublisher.publishTicketOverdue(
                            new TicketOverdueEvent(ticket.getId(), "RESOLUTION_OVERDUE", overdueMin, ticket.getAssignedTo(), ticket.getPriority()));
                    detected++;
                }
            }
        }

        if (detected > 0) {
            log.info("Overdue check complete: {} overdue tickets detected", detected);
        }
    }
}
