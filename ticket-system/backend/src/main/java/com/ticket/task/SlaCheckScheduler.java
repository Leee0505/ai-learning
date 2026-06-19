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

        // 1. Unassigned OPEN tickets past response SLA — filter in SQL
        LambdaQueryWrapper<Ticket> unassignedWrapper = new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getStatus, "OPEN")
                .isNull(Ticket::getAssignedTo())
                .and(w -> buildOverdueFilter(w, slas, now, false));
        List<Ticket> unassigned = ticketMapper.selectList(unassignedWrapper);

        for (Ticket ticket : unassigned) {
            SlaConfig sla = findSla(slas, ticket.getPriority());
            if (sla == null) continue;
            long deadline = ticket.getCreatedDate() + (long) sla.getResponseMinutes() * BusinessConstants.MILLIS_PER_MINUTE;
            int overdueMin = (int) ((now - deadline) / BusinessConstants.MILLIS_PER_MINUTE);
            eventPublisher.publishTicketOverdue(
                    new TicketOverdueEvent(ticket.getId(), "UNASSIGNED_OVERDUE", overdueMin, null, ticket.getPriority()));
            detected++;
        }

        // 2. Assigned OPEN/IN_PROGRESS tickets past resolution SLA — filter in SQL
        LambdaQueryWrapper<Ticket> assignedWrapper = new LambdaQueryWrapper<Ticket>()
                .in(Ticket::getStatus, "OPEN", "IN_PROGRESS")
                .isNotNull(Ticket::getAssignedTo())
                .and(w -> buildOverdueFilter(w, slas, now, true));
        List<Ticket> assigned = ticketMapper.selectList(assignedWrapper);

        for (Ticket ticket : assigned) {
            SlaConfig sla = findSla(slas, ticket.getPriority());
            if (sla == null) continue;
            long deadline = ticket.getCreatedDate() + (long) sla.getResolutionMinutes() * BusinessConstants.MILLIS_PER_MINUTE;
            int overdueMin = (int) ((now - deadline) / BusinessConstants.MILLIS_PER_MINUTE);
            eventPublisher.publishTicketOverdue(
                    new TicketOverdueEvent(ticket.getId(), "RESOLUTION_OVERDUE", overdueMin, ticket.getAssignedTo(), ticket.getPriority()));
            detected++;
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

    /**
     * Build a nested OR filter group: (priority=X AND created_date < deadline) OR (...)
     * First iteration uses eq+lt directly (no leading OR), subsequent use w.or().
     *
     * @param w             the nested wrapper from .and()
     * @param slas          loaded SLA configs
     * @param now           current time millis
     * @param useResolution true = resolution SLA, false = response SLA
     */
    private void buildOverdueFilter(LambdaQueryWrapper<Ticket> w, List<SlaConfig> slas, long now, boolean useResolution) {
        for (int i = 0; i < slas.size(); i++) {
            SlaConfig sla = slas.get(i);
            int minutes = useResolution ? sla.getResolutionMinutes() : sla.getResponseMinutes();
            long deadline = now - (long) minutes * BusinessConstants.MILLIS_PER_MINUTE;
            if (i == 0) {
                w.eq(Ticket::getPriority, sla.getPriority())
                 .lt(Ticket::getCreatedDate, deadline);
            } else {
                w.or(sub -> sub.eq(Ticket::getPriority, sla.getPriority())
                                .lt(Ticket::getCreatedDate, deadline));
            }
        }
    }
}
