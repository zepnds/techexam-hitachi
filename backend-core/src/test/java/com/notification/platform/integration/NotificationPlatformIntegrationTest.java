package com.notification.platform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.common.dto.NotificationResponse;
import com.notification.common.dto.NotificationStatusResponse;
import com.notification.common.dto.SendNotificationRequest;
import com.notification.common.model.ChannelType;
import com.notification.common.model.NotificationStatus;
import com.notification.platform.cep.state.WindowStateStore;
import com.notification.platform.channel.impl.EmailGatewayService;
import com.notification.platform.channel.impl.PushGatewayService;
import com.notification.platform.channel.impl.SlackGatewayService;
import com.notification.platform.channel.impl.SmsGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationPlatformIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WindowStateStore windowStateStore;

    @Autowired
    private EmailGatewayService emailGateway;

    @Autowired
    private SmsGatewayService smsGateway;

    @Autowired
    private PushGatewayService pushGateway;

    @Autowired
    private SlackGatewayService slackGateway;

    @BeforeEach
    void setUp() {
        windowStateStore.clear();
        emailGateway.setForceSuccess(true);
        smsGateway.setForceSuccess(true);
        pushGateway.setForceSuccess(true);
        slackGateway.setForceSuccess(true);
    }

    @Test
    @DisplayName("End-to-End: Should successfully accept, queue, process, and track an Email notification with JPA audit logs")
    void testEndToEndSuccessfulNotification() throws Exception {
        String uniqueMsg = "Your secure login token is " + UUID.randomUUID();
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId("12345")
                .country("UNITED_STATES")
                .channel(ChannelType.EMAIL)
                .message(uniqueMsg)
                .build();

        // 1. Submit POST /notifications
        MvcResult postResult = mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.user_id").value("12345"))
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andReturn();

        NotificationResponse response = objectMapper.readValue(
                postResult.getResponse().getContentAsString(),
                NotificationResponse.class
        );
        String notificationId = response.getId();

        // 2. Wait briefly for async worker to complete delivery
        Thread.sleep(250);

        // 3. Query GET /notifications/{id}/status
        MvcResult statusResult = mockMvc.perform(get("/notifications/" + notificationId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andReturn();

        NotificationStatusResponse statusResponse = objectMapper.readValue(
                statusResult.getResponse().getContentAsString(),
                NotificationStatusResponse.class
        );

        assertThat(statusResponse.getStatus()).isIn(NotificationStatus.QUEUED, NotificationStatus.PROCESSING, NotificationStatus.SENT, NotificationStatus.DELAYED);

        // 4. Query full notification details including JPA audit logs
        mockMvc.perform(get("/notifications/" + notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.audit_logs").isArray());
    }

    @Test
    @DisplayName("CEP Enforcement: Deduplication rule should DROP second identical message sent to same user")
    void testDeduplicationRuleIntegration() throws Exception {
        String userId = "user_dedup_test_" + UUID.randomUUID();
        String identicalMessage = "Password reset code 9988";

        SendNotificationRequest req1 = SendNotificationRequest.builder()
                .userId(userId)
                .country("UNITED_STATES")
                .channel(ChannelType.EMAIL)
                .message(identicalMessage)
                .build();

        // 1st request -> Accepted & Enqueued
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists());

        // 2nd identical request immediately -> DROPPED by CEP
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("DROPPED"))
                .andExpect(jsonPath("$.applied_cep_rule").value("RULE-DEDUP-001"))
                .andExpect(jsonPath("$.drop_reason").isNotEmpty());
    }

    @Test
    @DisplayName("CEP Enforcement: Rate limiting rule should DROP 4th SMS message within 1 hour")
    void testRateLimitSmsIntegration() throws Exception {
        String userId = "user_rate_test_" + UUID.randomUUID();

        for (int i = 1; i <= 3; i++) {
            SendNotificationRequest req = SendNotificationRequest.builder()
                    .userId(userId)
                    .country("UNITED_STATES")
                    .channel(ChannelType.SMS)
                    .message("Distinct SMS #" + i + " - " + UUID.randomUUID())
                    .build();

            mockMvc.perform(post("/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.id").exists());
        }

        // 4th SMS -> Should be DROPPED
        SendNotificationRequest req4 = SendNotificationRequest.builder()
                .userId(userId)
                .country("UNITED_STATES")
                .channel(ChannelType.SMS)
                .message("Distinct SMS #4 - " + UUID.randomUUID())
                .build();

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req4)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("DROPPED"))
                .andExpect(jsonPath("$.applied_cep_rule").value("RULE-RATE-LIMIT-SMS-002"));
    }

    @Test
    @DisplayName("Scheduling: Explicit schedule_at in future should set status to DELAYED")
    void testFutureSchedulingIntegration() throws Exception {
        Instant futureTime = Instant.now().plusSeconds(7200);
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId("sched_user_01")
                .channel(ChannelType.EMAIL)
                .message("Scheduled statement notice " + UUID.randomUUID())
                .scheduleAt(futureTime)
                .build();

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("DELAYED"))
                .andExpect(jsonPath("$.schedule_at").isNotEmpty());
    }

    @Test
    @DisplayName("Validation: Missing mandatory fields should return 400 Bad Request")
    void testValidationFailure() throws Exception {
        String invalidJson = "{\"country\": \"PHILIPPINES\"}";

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.userId").exists())
                .andExpect(jsonPath("$.validationErrors.message").exists());
    }

    @Test
    @DisplayName("Rules API: GET /notifications/rules should return loaded CEP rule DTOs")
    void testGetActiveRules() throws Exception {
        mockMvc.perform(get("/notifications/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].rule_type").exists());
    }

    @Test
    @DisplayName("Observability: Actuator Health endpoint should be UP")
    void testActuatorHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
