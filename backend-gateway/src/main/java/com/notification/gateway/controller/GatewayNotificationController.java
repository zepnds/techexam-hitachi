package com.notification.gateway.controller;

import com.notification.common.dto.*;
import com.notification.gateway.client.BackendCoreClient;
import com.notification.gateway.filter.EdgeCorrelationFilter;
import com.notification.gateway.filter.EdgeIdempotencyFilter;
import com.notification.gateway.idempotency.IdempotencyStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Reactive Backend Gateway API Ingress", description = "Spring WebFlux reactive entrypoint handling edge validation, non-blocking rate limiting, idempotency, correlation tracking, and WebClient reverse proxying")
public class GatewayNotificationController {

    private final BackendCoreClient backendCoreClient;
    private final IdempotencyStore idempotencyStore;

    @PostMapping
    @Operation(summary = "Submit Notification via Reactive Gateway", description = "Validates and non-blockingly proxies notification request payload to backend-core.")
    public Mono<ResponseEntity<NotificationResponse>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request,
            ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(EdgeCorrelationFilter.CORRELATION_ID_HEADER);
        String idempotencyHeader = exchange.getRequest().getHeaders().getFirst(EdgeIdempotencyFilter.IDEMPOTENCY_KEY_HEADER);
        if (idempotencyHeader == null) {
            idempotencyHeader = exchange.getRequest().getHeaders().getFirst(EdgeIdempotencyFilter.ALT_IDEMPOTENCY_KEY_HEADER);
        }
        
        final String idempotencyKey = request.getIdempotencyKey() != null ? request.getIdempotencyKey() : idempotencyHeader;

        log.info("[REACTIVE-GATEWAY-INGRESS] Received POST /notifications for user: {}, channel: {}, idempotencyKey: {}", 
                request.getUserId(), request.getChannel(), idempotencyKey);
        
        return backendCoreClient.sendNotification(request, correlationId)
                .doOnNext(response -> {
                    if (idempotencyKey != null && response != null) {
                        idempotencyStore.save(idempotencyKey, response);
                    }
                })
                .map(response -> ResponseEntity.status(HttpStatus.ACCEPTED).body(response));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Query Notification Status via Reactive Gateway")
    public Mono<ResponseEntity<NotificationStatusResponse>> getNotificationStatus(
            @Parameter(description = "Notification ID UUID") @PathVariable("id") String id,
            ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(EdgeCorrelationFilter.CORRELATION_ID_HEADER);
        return backendCoreClient.getNotificationStatus(id, correlationId)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Full Notification Entity Details via Reactive Gateway")
    public Mono<ResponseEntity<NotificationResponse>> getNotificationById(
            @Parameter(description = "Notification ID UUID") @PathVariable("id") String id,
            ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(EdgeCorrelationFilter.CORRELATION_ID_HEADER);
        return backendCoreClient.getNotification(id, correlationId)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List Notifications by User ID via Reactive Gateway")
    public Flux<NotificationResponse> getNotificationsByUserId(
            @PathVariable("userId") String userId,
            ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(EdgeCorrelationFilter.CORRELATION_ID_HEADER);
        return backendCoreClient.getNotificationsByUserId(userId, correlationId);
    }

    @GetMapping("/rules")
    @Operation(summary = "Inspect Active CEP Rules via Reactive Gateway")
    public Flux<CepRuleDto> getActiveRules(ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(EdgeCorrelationFilter.CORRELATION_ID_HEADER);
        return backendCoreClient.getActiveRules(correlationId);
    }

    @PostMapping("/rules/reload")
    @Operation(summary = "Trigger Zero-Downtime CEP Rule Reload via Reactive Gateway")
    public Mono<ResponseEntity<ReloadRulesResponse>> reloadRules(ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(EdgeCorrelationFilter.CORRELATION_ID_HEADER);
        return backendCoreClient.reloadRules(correlationId)
                .map(ResponseEntity::ok);
    }
}
