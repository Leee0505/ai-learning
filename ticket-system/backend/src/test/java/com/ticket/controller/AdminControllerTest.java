package com.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.common.constant.ErrorCode;
import com.ticket.config.TestConfig;
import com.ticket.dto.request.*;
import org.junit.jupiter.api.*;
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
class AdminControllerTest {

    private static final String TEST_PASSWORD = "Test@1234";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String adminJwt;
    private String userJwt;
    private Long adminId;

    @BeforeEach
    void setUp() throws Exception {
        seedUser("admin", "admin@test.local", "ROLE_ADMIN");
        seedUser("user1", "user1@test.local", "ROLE_USER");
        adminJwt = login("admin");
        userJwt = login("user1");
        adminId = getUserId("admin");
    }

    // ── List Users ──

    @Test
    void shouldListUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", auth(adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void shouldRejectListUsersForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", auth(userJwt)))
                .andExpect(status().isForbidden());
    }

    // ── Get User ──

    @Test
    void shouldGetUserById() throws Exception {
        mockMvc.perform(get("/api/admin/users/" + getUserId("user1"))
                        .header("Authorization", auth(adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("user1"));
    }

    // ── Create User ──

    @Test
    void shouldCreateUser() throws Exception {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setUsername("newuser");
        req.setEmail("newuser@test.local");
        req.setPassword("Pass@123");
        req.setRole("ROLE_USER");

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", auth(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("newuser"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"));
    }

    @Test
    void shouldRejectDuplicateUsername() throws Exception {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setUsername("user1"); // already seeded
        req.setEmail("other@test.local");
        req.setPassword("Pass@123");
        req.setRole("ROLE_USER");

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", auth(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_ALREADY_EXISTS.getCode()));
    }

    // ── Update User ──

    @Test
    void shouldUpdateUser() throws Exception {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setUsername("user1_renamed");

        mockMvc.perform(put("/api/admin/users/" + getUserId("user1"))
                        .header("Authorization", auth(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("user1_renamed"));
    }

    // ── Change Role ──

    @Test
    void shouldChangeRole() throws Exception {
        UserRoleUpdateRequest req = new UserRoleUpdateRequest();
        req.setRole("ROLE_AGENT");

        mockMvc.perform(patch("/api/admin/users/" + getUserId("user1") + "/role")
                        .header("Authorization", auth(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.role").value("ROLE_AGENT"));
    }

    @Test
    void shouldRejectChangeOwnRole() throws Exception {
        UserRoleUpdateRequest req = new UserRoleUpdateRequest();
        req.setRole("ROLE_USER");

        mockMvc.perform(patch("/api/admin/users/" + adminId + "/role")
                        .header("Authorization", auth(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.CANNOT_CHANGE_OWN_ROLE.getCode()));
    }

    // ── Toggle Status ──

    @Test
    void shouldToggleUserStatus() throws Exception {
        UserStatusUpdateRequest req = new UserStatusUpdateRequest();
        req.setStatus("0");

        mockMvc.perform(patch("/api/admin/users/" + getUserId("user1") + "/status")
                        .header("Authorization", auth(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ── Delete User ──

    @Test
    void shouldDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/admin/users/" + getUserId("user1"))
                        .header("Authorization", auth(adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldRejectDeleteSelf() throws Exception {
        mockMvc.perform(delete("/api/admin/users/" + adminId)
                        .header("Authorization", auth(adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.CANNOT_DELETE_SELF.getCode()));
    }

    // ── Helpers ──

    private void seedUser(String username, String email, String role) {
        String hash = passwordEncoder.encode(TEST_PASSWORD);
        long now = System.currentTimeMillis();
        jdbcTemplate.update(
            "INSERT INTO `user` (username, email, password, role, status, created_by, created_date) VALUES (?, ?, ?, ?, 1, 0, ?)",
            username, email, hash, role, now);
    }

    private String login(String username) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setLogin(username);
        req.setPassword(TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    private Long getUserId(String username) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM `user` WHERE username = ?", Long.class, username);
    }

    private String auth(String jwt) { return "Bearer " + jwt; }
}
