package com.ticket.task;

import com.ticket.config.TestConfig;
import com.ticket.entity.SlaConfig;
import com.ticket.entity.Ticket;
import com.ticket.event.EventPublisher;
import com.ticket.event.TicketOverdueEvent;
import com.ticket.mapper.SlaConfigMapper;
import com.ticket.mapper.TicketMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/sql/init-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Transactional
class SlaCheckSchedulerTest {

    @Autowired
    private SlaCheckScheduler scheduler;

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private SlaConfigMapper slaConfigMapper;

    @Autowired
    private EventPublisher eventPublisher; // Mocked by TestConfig

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Reset mock interactions before each test
        reset(eventPublisher);
        // Clear any existing tickets beyond seeds
        jdbcTemplate.update("DELETE FROM ticket");
    }

    // ── Detection: Unassigned Overdue (Response SLA) ──

    @Test
    void shouldDetectUnassignedOverdueTicket() {
        // Find URGENT SLA: response=60min
        SlaConfig urgentSla = slaConfigMapper.selectList(null).stream()
                .filter(s -> "URGENT".equals(s.getPriority()))
                .findFirst().orElseThrow();

        // Create an unassigned ticket created 2 hours ago (past 60min response SLA)
        Ticket ticket = new Ticket();
        ticket.setTenantId(1L);
        ticket.setTitle("Overdue unassigned");
        ticket.setStatus("OPEN");
        ticket.setPriority("URGENT");
        ticket.setAssignedTo(null);
        // Created 120 minutes ago → 60 min overdue
        ticket.setCreatedDate(System.currentTimeMillis() - 120 * 60 * 1000L);
        ticket.setCreatedBy(2L);
        ticketMapper.insert(ticket);

        scheduler.detectOverdueTickets();

        // Should publish an UNASSIGNED_OVERDUE event
        verify(eventPublisher, times(1)).publishTicketOverdue(any(TicketOverdueEvent.class));
    }

    @Test
    void shouldNotDetectUnassignedTicketWithinSla() {
        // Create an unassigned ticket created 10 minutes ago (within 60min URGENT SLA)
        Ticket ticket = new Ticket();
        ticket.setTenantId(1L);
        ticket.setTitle("Fresh unassigned");
        ticket.setStatus("OPEN");
        ticket.setPriority("URGENT");
        ticket.setAssignedTo(null);
        ticket.setCreatedDate(System.currentTimeMillis() - 10 * 60 * 1000L);
        ticket.setCreatedBy(2L);
        ticketMapper.insert(ticket);

        scheduler.detectOverdueTickets();

        // Should NOT publish any overdue event
        verify(eventPublisher, never()).publishTicketOverdue(any(TicketOverdueEvent.class));
    }

    // ── Detection: Assigned Overdue (Resolution SLA) ──

    @Test
    void shouldDetectAssignedOverdueTicket() {
        // URGENT SLA: resolution=240min (4 hours)
        // Create an assigned ticket created 5 hours ago (past resolution SLA)
        Ticket ticket = new Ticket();
        ticket.setTenantId(1L);
        ticket.setTitle("Overdue assigned");
        ticket.setStatus("IN_PROGRESS");
        ticket.setPriority("URGENT");
        ticket.setAssignedTo(3L);
        ticket.setCreatedDate(System.currentTimeMillis() - 300 * 60 * 1000L);
        ticket.setCreatedBy(2L);
        ticketMapper.insert(ticket);

        scheduler.detectOverdueTickets();

        // Should publish a RESOLUTION_OVERDUE event
        verify(eventPublisher, times(1)).publishTicketOverdue(any(TicketOverdueEvent.class));
    }

    @Test
    void shouldNotDetectAssignedTicketWithinResolutionSla() {
        // URGENT SLA: resolution=240min. Ticket created 1 hour ago → still within SLA.
        Ticket ticket = new Ticket();
        ticket.setTenantId(1L);
        ticket.setTitle("In-progress within SLA");
        ticket.setStatus("IN_PROGRESS");
        ticket.setPriority("URGENT");
        ticket.setAssignedTo(3L);
        ticket.setCreatedDate(System.currentTimeMillis() - 60 * 60 * 1000L);
        ticket.setCreatedBy(2L);
        ticketMapper.insert(ticket);

        scheduler.detectOverdueTickets();

        verify(eventPublisher, never()).publishTicketOverdue(any(TicketOverdueEvent.class));
    }

    // ── Mixed Scenarios ──

    @Test
    void shouldDetectMultipleOverdueTicketsAcrossPriorities() {
        long now = System.currentTimeMillis();

        // URGENT unassigned — 2h old (past 60min response SLA)
        Ticket t1 = new Ticket();
        t1.setTenantId(1L);
        t1.setTitle("Urgent unassigned overdue");
        t1.setStatus("OPEN");
        t1.setPriority("URGENT");
        t1.setAssignedTo(null);
        t1.setCreatedDate(now - 120 * 60 * 1000L);
        t1.setCreatedBy(2L);
        ticketMapper.insert(t1);

        // HIGH assigned — 30h old (past 1440min=24h resolution SLA)
        Ticket t2 = new Ticket();
        t2.setTenantId(1L);
        t2.setTitle("High assigned overdue");
        t2.setStatus("IN_PROGRESS");
        t2.setPriority("HIGH");
        t2.setAssignedTo(3L);
        t2.setCreatedDate(now - 1800 * 60 * 1000L);
        t2.setCreatedBy(2L);
        ticketMapper.insert(t2);

        // MEDIUM unassigned — 3h old (within 480min=8h response SLA → NOT overdue)
        Ticket t3 = new Ticket();
        t3.setTenantId(1L);
        t3.setTitle("Medium unassigned ok");
        t3.setStatus("OPEN");
        t3.setPriority("MEDIUM");
        t3.setAssignedTo(null);
        t3.setCreatedDate(now - 180 * 60 * 1000L);
        t3.setCreatedBy(2L);
        ticketMapper.insert(t3);

        scheduler.detectOverdueTickets();

        // Should detect exactly 2 overdue tickets
        verify(eventPublisher, times(2)).publishTicketOverdue(any(TicketOverdueEvent.class));
    }

    // ── Edge Cases ──

    @Test
    void shouldSkipClosedTickets() {
        // Create a closed ticket that would be overdue if open
        Ticket ticket = new Ticket();
        ticket.setTenantId(1L);
        ticket.setTitle("Closed overdue");
        ticket.setStatus("CLOSED");
        ticket.setPriority("URGENT");
        ticket.setAssignedTo(null);
        ticket.setCreatedDate(System.currentTimeMillis() - 300 * 60 * 1000L);
        ticket.setCreatedBy(2L);
        ticketMapper.insert(ticket);

        scheduler.detectOverdueTickets();

        // Closed tickets should be skipped — status filter only includes OPEN and IN_PROGRESS
        verify(eventPublisher, never()).publishTicketOverdue(any(TicketOverdueEvent.class));
    }

    @Test
    void shouldSkipTicketWithUnknownPriority() {
        // Create a ticket with priority not in SLA config
        Ticket ticket = new Ticket();
        ticket.setTenantId(1L);
        ticket.setTitle("Unknown priority");
        ticket.setStatus("OPEN");
        ticket.setPriority("UNKNOWN");
        ticket.setAssignedTo(null);
        ticket.setCreatedDate(System.currentTimeMillis() - 300 * 60 * 1000L);
        ticket.setCreatedBy(2L);
        ticketMapper.insert(ticket);

        scheduler.detectOverdueTickets();

        // No SLA match → no event published
        verify(eventPublisher, never()).publishTicketOverdue(any(TicketOverdueEvent.class));
    }

    @Test
    void shouldHandleNoActiveTickets() {
        // No tickets at all
        scheduler.detectOverdueTickets();

        verify(eventPublisher, never()).publishTicketOverdue(any(TicketOverdueEvent.class));
    }

    @Test
    void shouldHandleNonOverdueAssignedTicket() {
        // Assigned ticket still within resolution SLA
        Ticket ticket = new Ticket();
        ticket.setTenantId(1L);
        ticket.setTitle("Fresh assigned");
        ticket.setStatus("IN_PROGRESS");
        ticket.setPriority("LOW"); // 5760min resolution = 4 days
        ticket.setAssignedTo(3L);
        ticket.setCreatedDate(System.currentTimeMillis() - 60 * 60 * 1000L); // 1h old
        ticket.setCreatedBy(2L);
        ticketMapper.insert(ticket);

        scheduler.detectOverdueTickets();

        // LOW SLA has 5760min resolution → 1h is well within
        verify(eventPublisher, never()).publishTicketOverdue(any(TicketOverdueEvent.class));
    }
}
