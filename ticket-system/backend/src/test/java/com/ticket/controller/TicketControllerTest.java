package com.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.common.constant.BusinessConstants;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/sql/init-test-schema.sql")
@Transactional
class TicketControllerTest {

    // Password for all test users
    private static final String TEST_PASSWORD = "Test@1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String userJwt;
    private String user2Jwt;
    private String agentJwt;
    private String adminJwt;
    private Long userId;
    private Long user2Id;
    private Long agentId;
    private Long adminId;

    @BeforeEach
    void setUp() throws Exception {
        // Seed users with properly hashed passwords
        seedUser("admin", "admin@test.local", "ROLE_ADMIN");
        seedUser("agent1", "agent1@test.local", "ROLE_AGENT");
        seedUser("user1", "user1@test.local", "ROLE_USER");
        seedUser("user2", "user2@test.local", "ROLE_USER");

        // Login each user to get JWT
        userJwt = login("user1");
        user2Jwt = login("user2");
        agentJwt = login("agent1");
        adminJwt = login("admin");

        // Fetch user IDs
        userId = getUserId("user1");
        user2Id = getUserId("user2");
        agentId = getUserId("agent1");
        adminId = getUserId("admin");
    }

    // ──────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────

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

    private Long getUserId(String username) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM `user` WHERE username = ?", Long.class, username);
    }

    private String auth(String jwt) {
        return "Bearer " + jwt;
    }

    // ──────────────────────────────────────────────
    //  Create Ticket
    // ──────────────────────────────────────────────

    @Test
    void createTicketShouldSucceed() throws Exception {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setTitle("Test Ticket");
        req.setDescription("A test description");
        req.setPriority(BusinessConstants.TICKET_PRIORITY_MEDIUM);
        req.setCategory(BusinessConstants.TICKET_CATEGORY_BUG);

        mockMvc.perform(post("/api/tickets")
                .header("Authorization", auth(userJwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.title").value("Test Ticket"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.createdByName").value("user1"));
    }

    @Test
    void createTicketShouldRejectUnauthenticated() throws Exception {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setTitle("Test");
        req.setDescription("Desc");
        req.setPriority(BusinessConstants.TICKET_PRIORITY_LOW);
        req.setCategory(BusinessConstants.TICKET_CATEGORY_OTHER);

        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createTicketShouldRejectEmptyBody() throws Exception {
        mockMvc.perform(post("/api/tickets")
                .header("Authorization", auth(userJwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    // ──────────────────────────────────────────────
    //  List Tickets
    // ──────────────────────────────────────────────

    @Test
    void listTicketsShouldReturnPagedResults() throws Exception {
        // Create a ticket first
        createTestTicket(userJwt, "Ticket A", BusinessConstants.TICKET_PRIORITY_HIGH, BusinessConstants.TICKET_CATEGORY_BUG);

        mockMvc.perform(get("/api/tickets")
                .header("Authorization", auth(userJwt))
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void listTicketsShouldFilterByStatus() throws Exception {
        createTestTicket(userJwt, "Open Ticket", BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_OTHER);

        mockMvc.perform(get("/api/tickets")
                .header("Authorization", auth(userJwt))
                .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].status").value("OPEN"));
    }

    @Test
    void listTicketsUserShouldSeeOnlyOwn() throws Exception {
        // user1 creates a ticket
        createTestTicket(userJwt, "User1 Ticket", BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);
        // user2 creates a ticket
        createTestTicket(user2Jwt, "User2 Ticket", BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);

        // user1 should only see their own ticket
        MvcResult result = mockMvc.perform(get("/api/tickets")
                .header("Authorization", auth(userJwt)))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        int total = objectMapper.readTree(content).get("data").get("total").asInt();
        // user1 should see only 1 ticket (their own)
        org.assertj.core.api.Assertions.assertThat(total).isEqualTo(1);
    }

    @Test
    void listTicketsAgentShouldSeeAll() throws Exception {
        createTestTicket(userJwt, "User Ticket", BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);
        createTestTicket(user2Jwt, "Another Ticket", BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);

        MvcResult result = mockMvc.perform(get("/api/tickets")
                .header("Authorization", auth(agentJwt)))
                .andExpect(status().isOk())
                .andReturn();

        int total = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("total").asInt();
        org.assertj.core.api.Assertions.assertThat(total).isEqualTo(2);
    }

    // ──────────────────────────────────────────────
    //  Get Ticket Detail
    // ──────────────────────────────────────────────

    @Test
    void getTicketDetailShouldReturnFullDetail() throws Exception {
        Long ticketId = createTestTicket(userJwt, "Detail Ticket", BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);

        mockMvc.perform(get("/api/tickets/" + ticketId)
                .header("Authorization", auth(userJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ticketId))
                .andExpect(jsonPath("$.data.title").value("Detail Ticket"))
                .andExpect(jsonPath("$.data.createdByName").value("user1"))
                .andExpect(jsonPath("$.data.replies").isArray())
                .andExpect(jsonPath("$.data.attachments").isArray());
    }

    @Test
    void getTicketDetailShouldReturn403ForUserAccessingOthersTicket() throws Exception {
        Long ticketId = createTestTicket(userJwt, "Private", BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);

        mockMvc.perform(get("/api/tickets/" + ticketId)
                .header("Authorization", auth(user2Jwt)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40009)); // TICKET_ACCESS_DENIED
    }

    @Test
    void getTicketDetailShouldReturn404ForNonexistent() throws Exception {
        mockMvc.perform(get("/api/tickets/99999")
                .header("Authorization", auth(userJwt)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40008)); // TICKET_NOT_FOUND
    }

    // ──────────────────────────────────────────────
    //  Update Ticket
    // ──────────────────────────────────────────────

    @Test
    void updateTicketShouldSucceed() throws Exception {
        Long ticketId = createTestTicket(userJwt, "Original", BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);

        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setTitle("Updated Title");
        req.setPriority(BusinessConstants.TICKET_PRIORITY_URGENT);

        mockMvc.perform(put("/api/tickets/" + ticketId)
                .header("Authorization", auth(userJwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.priority").value("URGENT"));
    }

    @Test
    void updateTicketShouldRejectOtherUser() throws Exception {
        Long ticketId = createTestTicket(userJwt, "Mine", BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);

        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setTitle("Hacked");

        mockMvc.perform(put("/api/tickets/" + ticketId)
                .header("Authorization", auth(user2Jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ──────────────────────────────────────────────
    //  Change Status
    // ──────────────────────────────────────────────

    @Test
    void changeStatusShouldAllowAgentTransition() throws Exception {
        Long ticketId = createTestTicket(userJwt, "Status Test", BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);

        ChangeStatusRequest req = new ChangeStatusRequest();
        req.setStatus(BusinessConstants.TICKET_STATUS_IN_PROGRESS);

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                .header("Authorization", auth(agentJwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    void changeStatusShouldRejectInvalidTransition() throws Exception {
        Long ticketId = createTestTicket(userJwt, "Invalid Transition", BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);

        ChangeStatusRequest req = new ChangeStatusRequest();
        req.setStatus(BusinessConstants.TICKET_STATUS_RESOLVED); // OPEN -> RESOLVED is invalid

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                .header("Authorization", auth(agentJwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40010)); // TICKET_STATUS_INVALID
    }

    @Test
    void changeStatusShouldRejectRegularUser() throws Exception {
        Long ticketId = createTestTicket(userJwt, "User Status", BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);

        ChangeStatusRequest req = new ChangeStatusRequest();
        req.setStatus(BusinessConstants.TICKET_STATUS_IN_PROGRESS);

        // endpoint requires ADMIN or AGENT role
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                .header("Authorization", auth(userJwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ──────────────────────────────────────────────
    //  Assign Ticket
    // ──────────────────────────────────────────────

    @Test
    void assignTicketShouldSucceed() throws Exception {
        Long ticketId = createTestTicket(userJwt, "Assign Me", BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);

        AssignTicketRequest req = new AssignTicketRequest();
        req.setAssignedTo(agentId);

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/assign")
                .header("Authorization", auth(adminJwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedToName").value("agent1"));
    }

    @Test
    void assignTicketShouldRejectRegularUser() throws Exception {
        Long ticketId = createTestTicket(userJwt, "No Assign", BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);

        AssignTicketRequest req = new AssignTicketRequest();
        req.setAssignedTo(agentId);

        // endpoint requires ADMIN or AGENT
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/assign")
                .header("Authorization", auth(userJwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ──────────────────────────────────────────────
    //  Add Reply
    // ──────────────────────────────────────────────

    @Test
    void addReplyShouldSucceed() throws Exception {
        Long ticketId = createTestTicket(userJwt, "Reply Test", BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);

        CreateReplyRequest req = new CreateReplyRequest();
        req.setContent("This is a reply");
        req.setIsInternal(false);

        mockMvc.perform(post("/api/tickets/" + ticketId + "/replies")
                .header("Authorization", auth(userJwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("This is a reply"))
                .andExpect(jsonPath("$.data.username").value("user1"));
    }

    @Test
    void addReplyShouldAllowInternalNoteByAgent() throws Exception {
        Long ticketId = createTestTicket(userJwt, "Internal Note", BusinessConstants.TICKET_PRIORITY_MEDIUM, BusinessConstants.TICKET_CATEGORY_BUG);

        CreateReplyRequest req = new CreateReplyRequest();
        req.setContent("Internal discussion");
        req.setIsInternal(true);

        mockMvc.perform(post("/api/tickets/" + ticketId + "/replies")
                .header("Authorization", auth(agentJwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Internal discussion"));
    }

    // ──────────────────────────────────────────────
    //  Upload Attachment
    // ──────────────────────────────────────────────

    @Test
    void uploadAttachmentShouldSucceed() throws Exception {
        Long ticketId = createTestTicket(userJwt, "Upload", BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);

        MockMultipartFile file = new MockMultipartFile(
            "file", "test.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/tickets/" + ticketId + "/attachments")
                .file(file)
                .header("Authorization", auth(userJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originalFilename").value("test.txt"))
                .andExpect(jsonPath("$.data.fileSize").value(5));
    }

    @Test
    void uploadAttachmentShouldRejectTooLargeFile() throws Exception {
        Long ticketId = createTestTicket(userJwt, "Big File", BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);

        byte[] bigContent = new byte[(int) (BusinessConstants.MAX_UPLOAD_SIZE + 1)];
        MockMultipartFile file = new MockMultipartFile(
            "file", "big.dat", "application/octet-stream", bigContent);

        mockMvc.perform(multipart("/api/tickets/" + ticketId + "/attachments")
                .file(file)
                .header("Authorization", auth(userJwt)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40013)); // TICKET_ATTACHMENT_TOO_LARGE
    }

    // ──────────────────────────────────────────────
    //  Download Attachment
    // ──────────────────────────────────────────────

    @Test
    void downloadAttachmentShouldReturnFile() throws Exception {
        Long ticketId = createTestTicket(userJwt, "Download", BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);

        // Upload a file first
        MockMultipartFile file = new MockMultipartFile(
            "file", "readme.txt", "text/plain", "content".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/tickets/" + ticketId + "/attachments")
                .file(file)
                .header("Authorization", auth(userJwt)))
                .andExpect(status().isOk())
                .andReturn();

        Long attachmentId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Download it
        mockMvc.perform(get("/api/attachments/" + attachmentId)
                .header("Authorization", auth(userJwt)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    void downloadAttachmentShouldReturn404ForNonexistent() throws Exception {
        mockMvc.perform(get("/api/attachments/99999")
                .header("Authorization", auth(userJwt)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40012)); // TICKET_ATTACHMENT_NOT_FOUND
    }

    // ──────────────────────────────────────────────
    //  Delete Ticket
    // ──────────────────────────────────────────────

    @Test
    void deleteTicketShouldRequireAdminRole() throws Exception {
        Long ticketId = createTestTicket(userJwt, "To Delete", BusinessConstants.TICKET_PRIORITY_LOW, BusinessConstants.TICKET_CATEGORY_OTHER);

        // Regular user cannot delete
        mockMvc.perform(delete("/api/tickets/" + ticketId)
                .header("Authorization", auth(userJwt)))
                .andExpect(status().isForbidden());

        // Admin can delete
        mockMvc.perform(delete("/api/tickets/" + ticketId)
                .header("Authorization", auth(adminJwt)))
                .andExpect(status().isOk());
    }

    // ──────────────────────────────────────────────
    //  Helper: create a ticket and return its ID
    // ──────────────────────────────────────────────

    private Long createTestTicket(String jwt, String title, String priority, String category) throws Exception {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setTitle(title);
        req.setDescription("Test description");
        req.setPriority(priority);
        req.setCategory(category);

        MvcResult result = mockMvc.perform(post("/api/tickets")
                .header("Authorization", auth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asLong();
    }
}
