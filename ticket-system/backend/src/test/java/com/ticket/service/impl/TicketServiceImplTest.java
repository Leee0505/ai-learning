package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.constant.BusinessConstants;
import com.ticket.common.constant.RoleConstants;
import com.ticket.common.exception.BusinessException;
import com.ticket.common.exception.TicketAccessDeniedException;
import com.ticket.common.exception.TicketNotFoundException;
import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import com.ticket.entity.*;
import com.ticket.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class TicketServiceImplTest {

    private TicketServiceImpl service;
    private TicketMapper ticketMapper;
    private TicketReplyMapper ticketReplyMapper;
    private TicketAttachmentMapper ticketAttachmentMapper;
    private UserMapper userMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        ticketMapper = mock(TicketMapper.class);
        ticketReplyMapper = mock(TicketReplyMapper.class);
        ticketAttachmentMapper = mock(TicketAttachmentMapper.class);
        userMapper = mock(UserMapper.class);

        service = new TicketServiceImpl(ticketMapper, ticketReplyMapper, ticketAttachmentMapper, userMapper);

        // Inject temp upload path
        Field uploadPathField = TicketServiceImpl.class.getDeclaredField("uploadPath");
        uploadPathField.setAccessible(true);
        uploadPathField.set(service, tempDir.toString());
    }

    // ──────────────────────────────────────────────
    //  Helper factories
    // ──────────────────────────────────────────────

    private Ticket createTicketEntity(Long id, Long createdBy, String status, String priority, String category) {
        Ticket t = new Ticket();
        t.setId(id);
        t.setTitle("Test Ticket " + id);
        t.setDescription("Description " + id);
        t.setStatus(status);
        t.setPriority(priority);
        t.setCategory(category);
        t.setCreatedBy(createdBy);
        t.setCreatedDate(1700000000000L);
        t.setAssignedTo(null);
        return t;
    }

    private User createUser(Long id, String username, String role) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setRole(role);
        return u;
    }

    private CreateTicketRequest createTicketRequest(String title, String priority, String category) {
        CreateTicketRequest r = new CreateTicketRequest();
        r.setTitle(title);
        r.setDescription("Test description");
        r.setPriority(priority);
        r.setCategory(category);
        return r;
    }

    // ──────────────────────────────────────────────
    //  createTicket
    // ──────────────────────────────────────────────

    @Test
    void createTicketShouldSucceedWithValidInput() {
        CreateTicketRequest request = createTicketRequest("Bug report", BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        Long userId = 1L;

        when(ticketMapper.insert(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(100L);
            return 1;
        });
        User creator = createUser(userId, "testuser", RoleConstants.ROLE_USER);
        when(userMapper.selectById(userId)).thenReturn(creator);

        TicketDetailResponse response = service.createTicket(request, userId);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTitle()).isEqualTo("Bug report");
        assertThat(response.getStatus()).isEqualTo(BusinessConstants.TICKET_STATUS_OPEN);
        assertThat(response.getPriority()).isEqualTo(BusinessConstants.TICKET_PRIORITY_MEDIUM);
        assertThat(response.getCategory()).isEqualTo(BusinessConstants.TICKET_CATEGORY_BUG);
        assertThat(response.getCreatedByName()).isEqualTo("testuser");
        assertThat(response.getReplies()).isEmpty();
        assertThat(response.getAttachments()).isEmpty();
    }

    @Test
    void createTicketShouldThrowForInvalidPriority() {
        CreateTicketRequest request = createTicketRequest("Test", "INVALID_PRIORITY", BusinessConstants.TICKET_CATEGORY_BUG);

        assertThatThrownBy(() -> service.createTicket(request, 1L))
                .isInstanceOf(BusinessException.class);
        verify(ticketMapper, never()).insert(any());
    }

    @Test
    void createTicketShouldThrowForInvalidCategory() {
        CreateTicketRequest request = createTicketRequest("Test", BusinessConstants.TICKET_PRIORITY_LOW, "INVALID_CAT");

        assertThatThrownBy(() -> service.createTicket(request, 1L))
                .isInstanceOf(BusinessException.class);
        verify(ticketMapper, never()).insert(any());
    }

    // ──────────────────────────────────────────────
    //  listTickets
    // ──────────────────────────────────────────────

    @Test
    void listTicketsShouldReturnUserOwnTickets() {
        Long userId = 1L;
        String role = RoleConstants.ROLE_USER;

        Ticket t1 = createTicketEntity(1L, userId, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        Page<Ticket> pageResult = new Page<>(1, 20);
        pageResult.setRecords(List.of(t1));
        pageResult.setTotal(1);

        when(ticketMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(createUser(userId, "testuser", role)));

        PageResponse<TicketResponse> response = service.listTickets(null, null, null, null, 1, 20, userId, role);

        assertThat(response.getRecords()).hasSize(1);
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getRecords().get(0).getCreatedByName()).isEqualTo("testuser");
    }

    @Test
    void listTicketsShouldReturnAllForAgent() {
        Long agentId = 2L;
        String role = RoleConstants.ROLE_AGENT;

        Ticket t1 = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_HIGH, BusinessConstants.TICKET_CATEGORY_BUG);
        Ticket t2 = createTicketEntity(2L, 3L, BusinessConstants.TICKET_STATUS_IN_PROGRESS,
                BusinessConstants.TICKET_PRIORITY_URGENT, BusinessConstants.TICKET_CATEGORY_ACCOUNT_ISSUE);
        Page<Ticket> pageResult = new Page<>(1, 20);
        pageResult.setRecords(List.of(t1, t2));
        pageResult.setTotal(2);

        when(ticketMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);
        when(userMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(createUser(1L, "user1", RoleConstants.ROLE_USER),
                                    createUser(3L, "user2", RoleConstants.ROLE_USER)));

        PageResponse<TicketResponse> response = service.listTickets(null, null, null, null, 1, 20, agentId, role);

        assertThat(response.getRecords()).hasSize(2);
        assertThat(response.getTotal()).isEqualTo(2);
    }

    @Test
    void listTicketsShouldFilterByStatus() {
        Ticket t1 = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        Page<Ticket> pageResult = new Page<>(1, 20);
        pageResult.setRecords(List.of(t1));
        pageResult.setTotal(1);

        when(ticketMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);
        when(userMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(createUser(1L, "user1", RoleConstants.ROLE_USER)));

        PageResponse<TicketResponse> response = service.listTickets(
                BusinessConstants.TICKET_STATUS_OPEN, null, null, null, 1, 20, 2L, RoleConstants.ROLE_AGENT);

        assertThat(response.getRecords()).hasSize(1);
        assertThat(response.getRecords().get(0).getStatus()).isEqualTo(BusinessConstants.TICKET_STATUS_OPEN);
    }

    @Test
    void listTicketsShouldReturnEmptyPage() {
        Page<Ticket> emptyPage = new Page<>(1, 20);
        emptyPage.setRecords(Collections.emptyList());
        emptyPage.setTotal(0);

        when(ticketMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(emptyPage);

        PageResponse<TicketResponse> response = service.listTickets(null, null, null, null, 1, 20, 1L, RoleConstants.ROLE_USER);

        assertThat(response.getRecords()).isEmpty();
        assertThat(response.getTotal()).isEqualTo(0);
    }

    @Test
    void listTicketsShouldHandleNullAssignedTo() {
        Ticket t1 = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);
        t1.setAssignedTo(null); // Explicit null
        Page<Ticket> pageResult = new Page<>(1, 20);
        pageResult.setRecords(List.of(t1));
        pageResult.setTotal(1);

        when(ticketMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);
        when(userMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(createUser(1L, "user1", RoleConstants.ROLE_USER)));

        PageResponse<TicketResponse> response = service.listTickets(null, null, null, null, 1, 20, 2L, RoleConstants.ROLE_AGENT);

        assertThat(response.getRecords()).hasSize(1);
        assertThat(response.getRecords().get(0).getAssignedToName()).isNull();
    }

    // ──────────────────────────────────────────────
    //  getTicketDetail
    // ──────────────────────────────────────────────

    @Test
    void getTicketDetailShouldReturnFullDetail() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        ticket.setAssignedTo(2L);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        User creator = createUser(1L, "user1", RoleConstants.ROLE_USER);
        User assignee = createUser(2L, "agent1", RoleConstants.ROLE_AGENT);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(creator, assignee));

        TicketReply reply = new TicketReply();
        reply.setId(10L);
        reply.setTicketId(1L);
        reply.setUserId(1L);
        reply.setContent("A reply");
        reply.setIsInternal(0);
        reply.setCreatedDate(1700000000000L);
        when(ticketReplyMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(reply));

        TicketAttachment att = new TicketAttachment();
        att.setId(20L);
        att.setTicketId(1L);
        att.setOriginalFilename("screenshot.png");
        att.setFileSize(1024L);
        att.setContentType("image/png");
        att.setCreatedDate(1700000000000L);
        when(ticketAttachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(att));

        TicketDetailResponse response = service.getTicketDetail(1L, 1L, RoleConstants.ROLE_USER);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCreatedByName()).isEqualTo("user1");
        assertThat(response.getAssignedToName()).isEqualTo("agent1");
        assertThat(response.getReplies()).hasSize(1);
        assertThat(response.getReplies().get(0).getUsername()).isEqualTo("user1");
        assertThat(response.getAttachments()).hasSize(1);
        assertThat(response.getAttachments().get(0).getOriginalFilename()).isEqualTo("screenshot.png");
    }

    @Test
    void getTicketDetailShouldFilterInternalNotesForUser() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_IN_PROGRESS,
                BusinessConstants.TICKET_PRIORITY_HIGH, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(createUser(1L, "user1", RoleConstants.ROLE_USER)));

        // Mix of public and internal replies
        TicketReply publicReply = new TicketReply();
        publicReply.setId(10L); publicReply.setTicketId(1L); publicReply.setUserId(1L);
        publicReply.setContent("public"); publicReply.setIsInternal(0);
        TicketReply internalNote = new TicketReply();
        internalNote.setId(11L); internalNote.setTicketId(1L); internalNote.setUserId(2L);
        internalNote.setContent("secret"); internalNote.setIsInternal(1);
        when(ticketReplyMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(publicReply, internalNote));

        when(ticketAttachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // USER sees only public reply
        TicketDetailResponse userResp = service.getTicketDetail(1L, 1L, RoleConstants.ROLE_USER);
        assertThat(userResp.getReplies()).hasSize(1);
        assertThat(userResp.getReplies().get(0).getContent()).isEqualTo("public");

        // AGENT sees both
        TicketDetailResponse agentResp = service.getTicketDetail(1L, 2L, RoleConstants.ROLE_AGENT);
        assertThat(agentResp.getReplies()).hasSize(2);
    }

    @Test
    void getTicketDetailShouldThrowWhenNotFound() {
        when(ticketMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.getTicketDetail(999L, 1L, RoleConstants.ROLE_USER))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void getTicketDetailShouldThrowAccessDeniedForUserViewingOthersTicket() {
        // Ticket created by user 1, but user 2 tries to view it
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        assertThatThrownBy(() -> service.getTicketDetail(1L, 2L, RoleConstants.ROLE_USER))
                .isInstanceOf(TicketAccessDeniedException.class);
    }

    @Test
    void getTicketDetailShouldAllowAgentToAccessAnyTicket() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(createUser(1L, "user1", RoleConstants.ROLE_USER)));
        when(ticketReplyMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(ticketAttachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // Agent accessing another user's ticket should not throw
        assertThatCode(() -> service.getTicketDetail(1L, 999L, RoleConstants.ROLE_AGENT))
                .doesNotThrowAnyException();
    }

    // ──────────────────────────────────────────────
    //  updateTicket
    // ──────────────────────────────────────────────

    @Test
    void updateTicketShouldUpdateFields() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketMapper.updateById(any(Ticket.class))).thenReturn(1);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(createUser(1L, "user1", RoleConstants.ROLE_USER)));
        when(ticketReplyMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(ticketAttachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated Desc");
        request.setPriority(BusinessConstants.TICKET_PRIORITY_URGENT);

        TicketDetailResponse response = service.updateTicket(1L, request, 1L, RoleConstants.ROLE_USER);

        assertThat(response.getTitle()).isEqualTo("Updated Title");
        assertThat(response.getPriority()).isEqualTo(BusinessConstants.TICKET_PRIORITY_URGENT);
    }

    @Test
    void updateTicketShouldThrowWhenNotFound() {
        when(ticketMapper.selectById(999L)).thenReturn(null);
        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setTitle("New");

        assertThatThrownBy(() -> service.updateTicket(999L, request, 1L, RoleConstants.ROLE_USER))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void updateTicketShouldThrowAccessDeniedForUser() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setTitle("New");

        assertThatThrownBy(() -> service.updateTicket(1L, request, 2L, RoleConstants.ROLE_USER))
                .isInstanceOf(TicketAccessDeniedException.class);
    }

    @Test
    void updateTicketShouldRejectInvalidPriority() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setPriority("INVALID");

        assertThatThrownBy(() -> service.updateTicket(1L, request, 1L, RoleConstants.ROLE_USER))
                .isInstanceOf(BusinessException.class);
    }

    // ──────────────────────────────────────────────
    //  deleteTicket
    // ──────────────────────────────────────────────

    @Test
    void deleteTicketShouldSucceed() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_CLOSED,
                BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketAttachmentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(ticketMapper.deleteById(1L)).thenReturn(1);

        assertThatCode(() -> service.deleteTicket(1L)).doesNotThrowAnyException();
        verify(ticketMapper).deleteById(1L);
    }

    @Test
    void deleteTicketShouldThrowWhenNotFound() {
        when(ticketMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteTicket(999L))
                .isInstanceOf(TicketNotFoundException.class);
        verify(ticketMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteTicketShouldCleanUpAttachmentFiles() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_CLOSED,
                BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        TicketAttachment att = new TicketAttachment();
        att.setId(20L);
        att.setStoragePath(tempDir.resolve("test-file.dat").toString());
        when(ticketAttachmentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(att));
        when(ticketMapper.deleteById(1L)).thenReturn(1);

        assertThatCode(() -> service.deleteTicket(1L)).doesNotThrowAnyException();
        // File didn't exist, so deleteIfExists is a no-op — should not throw
        verify(ticketMapper).deleteById(1L);
    }

    // ──────────────────────────────────────────────
    //  changeStatus
    // ──────────────────────────────────────────────

    @Test
    void changeStatusShouldAllowOpenToInProgress() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketMapper.updateById(any(Ticket.class))).thenReturn(1);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(createUser(1L, "user1", RoleConstants.ROLE_USER)));
        when(ticketReplyMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(ticketAttachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ChangeStatusRequest request = new ChangeStatusRequest();
        request.setStatus(BusinessConstants.TICKET_STATUS_IN_PROGRESS);

        TicketDetailResponse response = service.changeStatus(1L, request, 2L, RoleConstants.ROLE_AGENT);

        assertThat(response.getStatus()).isEqualTo(BusinessConstants.TICKET_STATUS_IN_PROGRESS);
    }

    @Test
    void changeStatusShouldAllowOpenToClosed() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketMapper.updateById(any(Ticket.class))).thenReturn(1);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(createUser(1L, "user1", RoleConstants.ROLE_USER)));
        when(ticketReplyMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(ticketAttachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ChangeStatusRequest request = new ChangeStatusRequest();
        request.setStatus(BusinessConstants.TICKET_STATUS_CLOSED);

        TicketDetailResponse response = service.changeStatus(1L, request, 1L, RoleConstants.ROLE_USER);

        assertThat(response.getStatus()).isEqualTo(BusinessConstants.TICKET_STATUS_CLOSED);
        assertThat(response.getClosedDate()).isNotNull();
    }

    @Test
    void changeStatusShouldAllowInProgressToResolved() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_IN_PROGRESS,
                BusinessConstants.TICKET_PRIORITY_HIGH, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketMapper.updateById(any(Ticket.class))).thenReturn(1);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(createUser(1L, "user1", RoleConstants.ROLE_USER)));
        when(ticketReplyMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(ticketAttachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ChangeStatusRequest request = new ChangeStatusRequest();
        request.setStatus(BusinessConstants.TICKET_STATUS_RESOLVED);

        TicketDetailResponse response = service.changeStatus(1L, request, 2L, RoleConstants.ROLE_AGENT);

        assertThat(response.getStatus()).isEqualTo(BusinessConstants.TICKET_STATUS_RESOLVED);
        assertThat(response.getResolvedDate()).isNotNull();
    }

    @Test
    void changeStatusShouldRejectOpenToResolved() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        ChangeStatusRequest request = new ChangeStatusRequest();
        request.setStatus(BusinessConstants.TICKET_STATUS_RESOLVED);

        assertThatThrownBy(() -> service.changeStatus(1L, request, 2L, RoleConstants.ROLE_AGENT))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void changeStatusShouldRejectClosedToOpen() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_CLOSED,
                BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        ChangeStatusRequest request = new ChangeStatusRequest();
        request.setStatus(BusinessConstants.TICKET_STATUS_OPEN);

        assertThatThrownBy(() -> service.changeStatus(1L, request, 2L, RoleConstants.ROLE_AGENT))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void changeStatusShouldThrowWhenNotFound() {
        when(ticketMapper.selectById(999L)).thenReturn(null);

        ChangeStatusRequest request = new ChangeStatusRequest();
        request.setStatus(BusinessConstants.TICKET_STATUS_IN_PROGRESS);

        assertThatThrownBy(() -> service.changeStatus(999L, request, 2L, RoleConstants.ROLE_AGENT))
                .isInstanceOf(TicketNotFoundException.class);
    }

    // ──────────────────────────────────────────────
    //  assignTicket
    // ──────────────────────────────────────────────

    @Test
    void assignTicketShouldSucceed() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketMapper.updateById(any(Ticket.class))).thenReturn(1);

        User agent = createUser(2L, "agent1", RoleConstants.ROLE_AGENT);
        when(userMapper.selectById(2L)).thenReturn(agent);

        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(agent, createUser(1L, "user1", RoleConstants.ROLE_USER)));
        when(ticketReplyMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(ticketAttachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        AssignTicketRequest request = new AssignTicketRequest();
        request.setAssignedTo(2L);

        TicketDetailResponse response = service.assignTicket(1L, request, 1L, RoleConstants.ROLE_ADMIN);

        assertThat(response.getAssignedToName()).isEqualTo("agent1");
    }

    @Test
    void assignTicketShouldRejectNonAgentTarget() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        User regularUser = createUser(2L, "regular", RoleConstants.ROLE_USER);
        when(userMapper.selectById(2L)).thenReturn(regularUser);

        AssignTicketRequest request = new AssignTicketRequest();
        request.setAssignedTo(2L);

        assertThatThrownBy(() -> service.assignTicket(1L, request, 1L, RoleConstants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void assignTicketShouldRejectNonexistentTarget() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(userMapper.selectById(999L)).thenReturn(null);

        AssignTicketRequest request = new AssignTicketRequest();
        request.setAssignedTo(999L);

        assertThatThrownBy(() -> service.assignTicket(1L, request, 1L, RoleConstants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void assignTicketShouldThrowWhenTicketNotFound() {
        when(ticketMapper.selectById(999L)).thenReturn(null);

        AssignTicketRequest request = new AssignTicketRequest();
        request.setAssignedTo(2L);

        assertThatThrownBy(() -> service.assignTicket(999L, request, 1L, RoleConstants.ROLE_ADMIN))
                .isInstanceOf(TicketNotFoundException.class);
    }

    // ──────────────────────────────────────────────
    //  addReply
    // ──────────────────────────────────────────────

    @Test
    void addReplyShouldSucceedAsPublic() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketReplyMapper.insert(any(TicketReply.class))).thenAnswer(inv -> {
            TicketReply r = inv.getArgument(0);
            r.setId(100L);
            return 1;
        });
        when(userMapper.selectById(1L)).thenReturn(createUser(1L, "user1", RoleConstants.ROLE_USER));

        CreateReplyRequest request = new CreateReplyRequest();
        request.setContent("My reply");
        request.setIsInternal(false);

        TicketReplyResponse response = service.addReply(1L, request, 1L);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getContent()).isEqualTo("My reply");
        assertThat(response.getUsername()).isEqualTo("user1");
    }

    @Test
    void addReplyShouldSucceedAsInternalNote() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_IN_PROGRESS,
                BusinessConstants.TICKET_PRIORITY_HIGH, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketReplyMapper.insert(any(TicketReply.class))).thenAnswer(inv -> {
            TicketReply r = inv.getArgument(0);
            r.setId(101L);
            return 1;
        });
        when(userMapper.selectById(2L)).thenReturn(createUser(2L, "agent1", RoleConstants.ROLE_AGENT));

        CreateReplyRequest request = new CreateReplyRequest();
        request.setContent("Internal note");
        request.setIsInternal(true);

        TicketReplyResponse response = service.addReply(1L, request, 2L);

        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getUsername()).isEqualTo("agent1");
    }

    @Test
    void addReplyShouldThrowWhenTicketNotFound() {
        when(ticketMapper.selectById(999L)).thenReturn(null);

        CreateReplyRequest request = new CreateReplyRequest();
        request.setContent("Reply");

        assertThatThrownBy(() -> service.addReply(999L, request, 1L))
                .isInstanceOf(TicketNotFoundException.class);
    }

    // ──────────────────────────────────────────────
    //  uploadAttachment
    // ──────────────────────────────────────────────

    @Test
    void uploadAttachmentShouldSucceed() throws IOException {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketAttachmentMapper.insert(any(TicketAttachment.class))).thenAnswer(inv -> {
            TicketAttachment a = inv.getArgument(0);
            a.setId(200L);
            return 1;
        });

        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("screenshot.png");
        when(file.getContentType()).thenReturn("image/png");

        TicketAttachmentResponse response = service.uploadAttachment(1L, file, 1L);

        assertThat(response.getId()).isEqualTo(200L);
        assertThat(response.getOriginalFilename()).isEqualTo("screenshot.png");
        assertThat(response.getFileSize()).isEqualTo(1024L);
        verify(file).transferTo(any(java.io.File.class));
    }

    @Test
    void uploadAttachmentShouldRejectTooLargeFile() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(BusinessConstants.MAX_UPLOAD_SIZE + 1);

        assertThatThrownBy(() -> service.uploadAttachment(1L, file, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(com.ticket.common.constant.ErrorCode.TICKET_ATTACHMENT_TOO_LARGE);
    }

    @Test
    void uploadAttachmentShouldRejectInvalidExtension() {
        Ticket ticket = createTicketEntity(1L, 1L, BusinessConstants.TICKET_STATUS_OPEN,
                BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(100L);
        when(file.getOriginalFilename()).thenReturn("malware.exe");

        assertThatThrownBy(() -> service.uploadAttachment(1L, file, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(com.ticket.common.constant.ErrorCode.TICKET_ATTACHMENT_TYPE_DENIED);
    }

    @Test
    void uploadAttachmentShouldThrowWhenTicketNotFound() {
        when(ticketMapper.selectById(999L)).thenReturn(null);

        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(100L);
        when(file.getOriginalFilename()).thenReturn("test.png");

        assertThatThrownBy(() -> service.uploadAttachment(999L, file, 1L))
                .isInstanceOf(TicketNotFoundException.class);
    }

    // ──────────────────────────────────────────────
    //  downloadAttachment
    // ──────────────────────────────────────────────

    @Test
    void downloadAttachmentShouldReturnResource() throws IOException {
        // Create a real file for download
        Path filePath = tempDir.resolve("test-attachment.dat");
        java.nio.file.Files.write(filePath, "test content".getBytes());

        TicketAttachment att = new TicketAttachment();
        att.setId(300L);
        att.setStoragePath(filePath.toString());
        att.setFilename("test-attachment.dat");
        when(ticketAttachmentMapper.selectById(300L)).thenReturn(att);

        org.springframework.core.io.Resource resource = service.downloadAttachment(300L);

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.getFilename()).isEqualTo("test-attachment.dat");
    }

    @Test
    void downloadAttachmentShouldThrowWhenDbRecordNotFound() {
        when(ticketAttachmentMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.downloadAttachment(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(com.ticket.common.constant.ErrorCode.TICKET_ATTACHMENT_NOT_FOUND);
    }

    @Test
    void downloadAttachmentShouldThrowWhenFileMissingOnDisk() {
        TicketAttachment att = new TicketAttachment();
        att.setId(301L);
        att.setStoragePath(tempDir.resolve("nonexistent.dat").toString());
        when(ticketAttachmentMapper.selectById(301L)).thenReturn(att);

        assertThatThrownBy(() -> service.downloadAttachment(301L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(com.ticket.common.constant.ErrorCode.TICKET_ATTACHMENT_NOT_FOUND);
    }
}
