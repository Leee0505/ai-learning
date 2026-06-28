package com.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.config.TestConfig;
import com.ticket.dto.request.*;
import com.ticket.entity.Notification;
import com.ticket.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/sql/init-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Transactional
class NotificationControllerTest {

    private static final String TEST_PASSWORD = "Test@1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NotificationMapper notificationMapper;

    private String userJwt;
    private Long userId;

    @BeforeEach
    void setUp() throws Exception {
        seedUser("user1", "user1@test.local", "ROLE_USER");
        userId = jdbcTemplate.queryForObject(
                "SELECT id FROM `user` WHERE username = 'user1'", Long.class);
        userJwt = login("user1");

        // Clear notifications for clean test state
        jdbcTemplate.update("DELETE FROM notification");
    }

    // ── List Notifications ──

    @Test
    void shouldListNotificationsEmpty() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", auth(userJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void shouldListNotificationsWithData() throws Exception {
        // Insert test notifications
        insertNotification(userId, "TICKET_CREATED", "New ticket: Bug report", false);
        insertNotification(userId, "TICKET_ASSIGNED", "Ticket #101 assigned", true);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", auth(userJwt))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].type").value("TICKET_CREATED")) // newest first
                .andExpect(jsonPath("$.data[1].type").value("TICKET_ASSIGNED"));
    }

    @Test
    void shouldListNotificationsWithPagination() throws Exception {
        // Insert 5 notifications
        for (int i = 1; i <= 5; i++) {
            insertNotification(userId, "TICKET_CREATED", "Ticket #" + i, false);
        }

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", auth(userJwt))
                        .param("page", "1")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(3)));
    }

    // ── Unread Count ──

    @Test
    void shouldGetUnreadCountZero() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", auth(userJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    void shouldGetUnreadCountWithUnread() throws Exception {
        insertNotification(userId, "TICKET_CREATED", "Unread 1", false);
        insertNotification(userId, "TICKET_ASSIGNED", "Unread 2", false);
        insertNotification(userId, "TICKET_CREATED", "Already read", true);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", auth(userJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(2));
    }

    // ── Mark Read ──

    @Test
    void shouldMarkSingleAsRead() throws Exception {
        insertNotification(userId, "TICKET_CREATED", "Mark me read", false);
        Long notifId = jdbcTemplate.queryForObject(
                "SELECT id FROM notification WHERE user_id = ? LIMIT 1", Long.class, userId);

        mockMvc.perform(put("/api/notifications/" + notifId + "/read")
                        .header("Authorization", auth(userJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify it's now read
        Integer isRead = jdbcTemplate.queryForObject(
                "SELECT is_read FROM notification WHERE id = ?", Integer.class, notifId);
        assert isRead == 1 : "Notification should be marked as read";
    }

    @Test
    void shouldNotMarkOthersNotificationAsRead() throws Exception {
        // Create another user's notification
        seedUser("user2", "user2@test.local", "ROLE_USER");
        Long otherUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM `user` WHERE username = 'user2'", Long.class);
        insertNotification(otherUserId, "TICKET_CREATED", "Other's notification", false);
        Long otherNotifId = jdbcTemplate.queryForObject(
                "SELECT id FROM notification WHERE user_id = ? LIMIT 1", Long.class, otherUserId);

        // Try to mark it as read with user1's token
        mockMvc.perform(put("/api/notifications/" + otherNotifId + "/read")
                        .header("Authorization", auth(userJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify it's NOT marked as read (service should reject)
        Integer isRead = jdbcTemplate.queryForObject(
                "SELECT is_read FROM notification WHERE id = ?", Integer.class, otherNotifId);
        assert isRead == 0 : "Other user's notification should NOT be marked as read";
    }

    @Test
    void shouldMarkAllAsRead() throws Exception {
        insertNotification(userId, "TICKET_CREATED", "Unread 1", false);
        insertNotification(userId, "TICKET_ASSIGNED", "Unread 2", false);
        insertNotification(userId, "TICKET_CREATED", "Already read", true);

        mockMvc.perform(put("/api/notifications/read-all")
                        .header("Authorization", auth(userJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify all are read
        Integer unreadCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = 0",
                Integer.class, userId);
        assert unreadCount == 0 : "All notifications should be marked as read";
    }

    // ── Access Control ──

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRequireAuthenticationForUnreadCount() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRequireAuthenticationForMarkRead() throws Exception {
        mockMvc.perform(put("/api/notifications/1/read"))
                .andExpect(status().isForbidden());
    }

    // ── Helpers ──

    private void seedUser(String username, String email, String role) {
        String hash = passwordEncoder.encode(TEST_PASSWORD);
        long now = System.currentTimeMillis();
        jdbcTemplate.update(
            "INSERT INTO `user` (username, email, password, role, status, created_by, created_date) VALUES (?, ?, ?, ?, 1, 0, ?)",
            username, email, hash, role, now);
    }

    private void insertNotification(Long userId, String type, String title, boolean isRead) {
        Notification n = new Notification();
        n.setTenantId(1L);
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setIsRead(isRead ? 1 : 0);
        n.setCreatedDate(System.currentTimeMillis() - (isRead ? 3600_000 : 0)); // read ones are older
        notificationMapper.insert(n);
    }

    private String login(String username) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setLogin(username);
        req.setPassword(TEST_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    private String auth(String jwt) {
        return "Bearer " + jwt;
    }
}
