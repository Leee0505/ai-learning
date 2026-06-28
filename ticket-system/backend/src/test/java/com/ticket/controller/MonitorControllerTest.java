package com.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.config.TestConfig;
import com.ticket.dto.request.*;
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

/**
 * Tests for MonitorController — system health endpoints.
 * Uses real MonitorServiceImpl which gracefully handles
 * unavailable Kafka/Redis (returns disconnected fallback).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/sql/init-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Transactional
class MonitorControllerTest {

    private static final String TEST_PASSWORD = "Test@1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminJwt;
    private String userJwt;

    @BeforeEach
    void setUp() throws Exception {
        seedUser("admin", "admin@test.local", "ROLE_ADMIN");
        seedUser("user1", "user1@test.local", "ROLE_USER");

        adminJwt = login("admin");
        userJwt = login("user1");
    }

    // ── Overview ──

    @Test
    void shouldGetOverview() throws Exception {
        mockMvc.perform(get("/api/admin/monitor/overview")
                        .header("Authorization", auth(adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.kafkaStatus").exists())
                .andExpect(jsonPath("$.data.redisStatus").exists())
                .andExpect(jsonPath("$.data.apiStatus").exists());
    }

    // ── Kafka ──

    @Test
    void shouldGetKafkaMetrics() throws Exception {
        mockMvc.perform(get("/api/admin/monitor/kafka")
                        .header("Authorization", auth(adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.connected").exists());
    }

    // ── Redis ──

    @Test
    void shouldGetRedisMetrics() throws Exception {
        mockMvc.perform(get("/api/admin/monitor/redis")
                        .header("Authorization", auth(adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.connected").exists());
    }

    // ── API Metrics ──

    @Test
    void shouldGetApiMetrics() throws Exception {
        mockMvc.perform(get("/api/admin/monitor/api")
                        .header("Authorization", auth(adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.requestCount").isNumber())
                .andExpect(jsonPath("$.data.p50Ms").isNumber())
                .andExpect(jsonPath("$.data.p95Ms").isNumber())
                .andExpect(jsonPath("$.data.p99Ms").isNumber());
    }

    // ── Access Control ──

    @Test
    void shouldDenyNonAdminAccess() throws Exception {
        mockMvc.perform(get("/api/admin/monitor/overview")
                        .header("Authorization", auth(userJwt)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyNonAdminAccessToKafka() throws Exception {
        mockMvc.perform(get("/api/admin/monitor/kafka")
                        .header("Authorization", auth(userJwt)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/monitor/overview"))
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
