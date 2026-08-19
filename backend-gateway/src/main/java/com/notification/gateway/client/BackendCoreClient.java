package com.notification.gateway.client;

import com.notification.common.dto.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BackendCoreClient {

    Mono<NotificationResponse> sendNotification(SendNotificationRequest request, String correlationId);

    Mono<NotificationStatusResponse> getNotificationStatus(String id, String correlationId);

    Mono<NotificationResponse> getNotification(String id, String correlationId);

    Flux<NotificationResponse> getNotificationsByUserId(String userId, String correlationId);

    Flux<CepRuleDto> getActiveRules(String correlationId);

    Mono<ReloadRulesResponse> reloadRules(String correlationId);
}
