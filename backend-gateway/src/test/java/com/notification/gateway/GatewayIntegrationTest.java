package com.notification.gateway;

import com.notification.common.dto.NotificationResponse;
import com.notification.common.dto.SendNotificationRequest;
import com.notification.common.model.ChannelType;
import com.notification.common.model.NotificationStatus;
import com.notification.gateway.client.BackendCoreClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private BackendCoreClient backendCoreClient;

    @Test
    @DisplayName("Reactive Gateway Ingress: Should accept payload, generate correlation ID, and forward to backend-core")
    void testGatewayNotificationIngress() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId("gw_user_01")
                .country("PHILIPPINES")
                .channel(ChannelType.EMAIL)
                .message("Gateway Ingress Test OTP 1234")
                .build();

        NotificationResponse mockResponse = NotificationResponse.builder()
                .id("gw-notif-uuid-1122")
                .userId("gw_user_01")
                .country("PHILIPPINES")
                .channel(ChannelType.EMAIL)
                .message("Gateway Ingress Test OTP 1234")
                .status(NotificationStatus.QUEUED)
                .createdAt(Instant.now())
                .build();

        when(backendCoreClient.sendNotification(any(), nullable(String.class)))
                .thenReturn(Mono.just(mockResponse));

        webTestClient.post()
                .uri("/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isAccepted()
                .expectHeader().exists("X-Correlation-Id")
                .expectBody()
                .jsonPath("$.id").isEqualTo("gw-notif-uuid-1122")
                .jsonPath("$.user_id").isEqualTo("gw_user_01")
                .jsonPath("$.status").isEqualTo("QUEUED");
    }

    @Test
    @DisplayName("Idempotency Engine: Duplicate POST with same Idempotency-Key should return cached response with X-Cache-Hit header")
    void testGatewayIdempotencyEngine() {
        String idempotencyKey = "idemp-key-test-990011";
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId("idemp_user")
                .channel(ChannelType.SMS)
                .message("Idempotency SMS Test")
                .idempotencyKey(idempotencyKey)
                .build();

        NotificationResponse mockResponse = NotificationResponse.builder()
                .id("notif-idemp-uuid-8899")
                .userId("idemp_user")
                .channel(ChannelType.SMS)
                .message("Idempotency SMS Test")
                .status(NotificationStatus.QUEUED)
                .createdAt(Instant.now())
                .build();

        when(backendCoreClient.sendNotification(any(), nullable(String.class)))
                .thenReturn(Mono.just(mockResponse));

        // First Request -> Cache Miss
        webTestClient.post()
                .uri("/notifications")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isAccepted()
                .expectHeader().valueEquals("Idempotency-Key", idempotencyKey)
                .expectBody()
                .jsonPath("$.id").isEqualTo("notif-idemp-uuid-8899");

        verify(backendCoreClient, times(1)).sendNotification(any(), nullable(String.class));

        // Second Identical Request -> Cache Hit
        webTestClient.post()
                .uri("/notifications")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isAccepted()
                .expectHeader().valueEquals("X-Cache-Hit", "true")
                .expectHeader().valueEquals("Idempotency-Key", idempotencyKey)
                .expectBody()
                .jsonPath("$.id").isEqualTo("notif-idemp-uuid-8899");

        verify(backendCoreClient, times(1)).sendNotification(any(), nullable(String.class));
    }
}
