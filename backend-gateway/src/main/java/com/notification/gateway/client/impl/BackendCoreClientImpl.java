package com.notification.gateway.client.impl;

import com.notification.common.dto.*;
import com.notification.gateway.client.BackendCoreClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackendCoreClientImpl implements BackendCoreClient {

    private final WebClient backendCoreWebClient;

    @Override
    public Mono<NotificationResponse> sendNotification(SendNotificationRequest request, String correlationId) {
        log.debug("Reactive forward POST /notifications for user: {}", request.getUserId());
        return backendCoreWebClient.post()
                .uri("/notifications")
                .header("X-Correlation-Id", correlationId != null ? correlationId : "")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(NotificationResponse.class);
    }

    @Override
    public Mono<NotificationStatusResponse> getNotificationStatus(String id, String correlationId) {
        log.debug("Reactive forward GET /notifications/{}/status", id);
        return backendCoreWebClient.get()
                .uri("/notifications/{id}/status", id)
                .header("X-Correlation-Id", correlationId != null ? correlationId : "")
                .retrieve()
                .bodyToMono(NotificationStatusResponse.class);
    }

    @Override
    public Mono<NotificationResponse> getNotification(String id, String correlationId) {
        log.debug("Reactive forward GET /notifications/{}", id);
        return backendCoreWebClient.get()
                .uri("/notifications/{id}", id)
                .header("X-Correlation-Id", correlationId != null ? correlationId : "")
                .retrieve()
                .bodyToMono(NotificationResponse.class);
    }

    @Override
    public Flux<NotificationResponse> getNotificationsByUserId(String userId, String correlationId) {
        log.debug("Reactive forward GET /notifications/user/{}", userId);
        return backendCoreWebClient.get()
                .uri("/notifications/user/{userId}", userId)
                .header("X-Correlation-Id", correlationId != null ? correlationId : "")
                .retrieve()
                .bodyToFlux(NotificationResponse.class);
    }

    @Override
    public Flux<CepRuleDto> getActiveRules(String correlationId) {
        log.debug("Reactive forward GET /notifications/rules");
        return backendCoreWebClient.get()
                .uri("/notifications/rules")
                .header("X-Correlation-Id", correlationId != null ? correlationId : "")
                .retrieve()
                .bodyToFlux(CepRuleDto.class);
    }

    @Override
    public Mono<ReloadRulesResponse> reloadRules(String correlationId) {
        log.info("Reactive forward POST /notifications/rules/reload");
        return backendCoreWebClient.post()
                .uri("/notifications/rules/reload")
                .header("X-Correlation-Id", correlationId != null ? correlationId : "")
                .retrieve()
                .bodyToMono(ReloadRulesResponse.class);
    }
}
