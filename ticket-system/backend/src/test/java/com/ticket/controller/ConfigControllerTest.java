package com.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.common.constant.ErrorCode;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/sql/init-test-schema.sql")
@Transactional
class ConfigControllerTest {

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
        // Seed users
        seedUser("admin", "admin@test.local", "ROLE_ADMIN");
        seedUser("user1", "user1@test.local", "ROLE_USER");

        // Login
        adminJwt = login("admin");
        userJwt = login("user1");
    }

    // ── Field Tests ──

    @Test
    void shouldListFields() throws Exception {
        mockMvc.perform(get("/api/admin/config/fields")
                        .header("Authorization", auth(adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2))); // Seeded 2 fields
    }

    @Test
    void shouldCreateField() throws Exception {
        CreateFieldRequest req = new CreateFieldRequest();
        req.setName("Test Field");
        req.setFieldKey("test_field");
        req.setFieldType("TEXT");
        req.setDisplayOrder(10);

        mockMvc.perform(post("/api/admin/config/fields")
                        .header("Authorization", auth(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Test Field"))
                .andExpect(jsonPath("$.data.fieldKey").value("test_field"));
    }

    @Test
    void shouldRejectDuplicateFieldKey() throws Exception {
        CreateFieldRequest req = new CreateFieldRequest();
        req.setName("Env Duplicate");
        req.setFieldKey("environment"); // Already seeded by init-test-schema
        req.setFieldType("TEXT");

        mockMvc.perform(post("/api/admin/config/fields")
                        .header("Authorization", auth(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.FIELD_KEY_DUPLICATE.getCode()));
    }

    @Test
    void shouldUpdateField() throws Exception {
        // Get the seeded field's ID
        Long fieldId = jdbcTemplate.queryForObject(
                "SELECT id FROM ticket_field_config WHERE field_key = 'version'", Long.class);

        UpdateFieldRequest req = new UpdateFieldRequest();
        req.setName("App Version");

        mockMvc.perform(put("/api/admin/config/fields/" + fieldId)
                        .header("Authorization", auth(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("App Version"));
    }

    @Test
    void shouldDeleteField() throws Exception {
        // First create one to delete
        CreateFieldRequest req = new CreateFieldRequest();
        req.setName("ToDelete");
        req.setFieldKey("to_delete");
        req.setFieldType("TEXT");

        MvcResult result = mockMvc.perform(post("/api/admin/config/fields")
                        .header("Authorization", auth(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        int id = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asInt();

        mockMvc.perform(delete("/api/admin/config/fields/" + id)
                        .header("Authorization", auth(adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ── SLA Tests ──

    @Test
    void shouldListSla() throws Exception {
        mockMvc.perform(get("/api/admin/config/sla")
                        .header("Authorization", auth(adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(4)));
    }

    @Test
    void shouldUpdateSla() throws Exception {
        // Get the URGENT SLA ID (seeded)
        Long slaId = jdbcTemplate.queryForObject(
                "SELECT id FROM sla_config WHERE priority = 'URGENT'", Long.class);

        UpdateSlaRequest req = new UpdateSlaRequest();
        req.setResponseMinutes(120);
        req.setResolutionMinutes(480);

        mockMvc.perform(put("/api/admin/config/sla/" + slaId)
                        .header("Authorization", auth(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.responseMinutes").value(120))
                .andExpect(jsonPath("$.data.resolutionMinutes").value(480));
    }

    @Test
    void shouldRejectSlaWhereResponseExceedsResolution() throws Exception {
        Long slaId = jdbcTemplate.queryForObject(
                "SELECT id FROM sla_config WHERE priority = 'URGENT'", Long.class);

        UpdateSlaRequest req = new UpdateSlaRequest();
        req.setResponseMinutes(600);
        req.setResolutionMinutes(300);

        mockMvc.perform(put("/api/admin/config/sla/" + slaId)
                        .header("Authorization", auth(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SLA_RESPONSE_MUST_BE_LESS_THAN_RESOLUTION.getCode()));
    }

    // ── Access Control ──

    @Test
    void shouldDenyNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/config/fields")
                        .header("Authorization", auth(userJwt)))
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
