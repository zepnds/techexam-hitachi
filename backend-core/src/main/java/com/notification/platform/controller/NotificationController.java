package com.notification.platform.controller;

import com.notification.common.dto.*;
import com.notification.platform.cep.service.CepRuleRegistry;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.dto.mapper.NotificationMapper;
import com.notification.platform.service.NotificationOrchestrator;
import com.notification.platform.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Core API", description = "Internal core business logic endpoints")
public class NotificationController {

    private final NotificationOrchestrator notificationOrchestrator;
    private final NotificationQueryService notificationQueryService;
    private final CepRuleRegistry cepRuleRegistry;
    private final NotificationMapper notificationMapper;

    @PostMapping
    @Operation(summary = "Send / Enqueue a Notification")
    public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody SendNotificationRequest request) {
        log.info("Received notification request for userId: {}, channel: {}", request.getUserId(), request.getChannel());
        Notification notification = notificationOrchestrator.processNotificationRequest(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(notificationMapper.toResponse(notification));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get Notification Status")
    public ResponseEntity<NotificationStatusResponse> getNotificationStatus(
            @Parameter(description = "Notification ID UUID") @PathVariable("id") String id) {
        return ResponseEntity.ok(notificationQueryService.getNotificationStatus(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Notification Details")
    public ResponseEntity<NotificationResponse> getNotificationById(
            @Parameter(description = "Notification ID UUID") @PathVariable("id") String id) {
        return ResponseEntity.ok(notificationQueryService.getNotification(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List Notifications by User ID")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByUserId(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(notificationQueryService.getNotificationsByUserId(userId));
    }

    @GetMapping("/rules")
    @Operation(summary = "Inspect Active CEP Rules")
    public ResponseEntity<List<CepRuleDto>> getActiveRules() {
        return ResponseEntity.ok(notificationMapper.toCepRuleDtoList(cepRuleRegistry.getActiveRules()));
    }

    @PostMapping("/rules/reload")
    @Operation(summary = "Hot-Reload CEP Rules")
    public ResponseEntity<ReloadRulesResponse> reloadRules() {
        cepRuleRegistry.loadRules();
        return ResponseEntity.ok(ReloadRulesResponse.builder()
                .status("SUCCESS")
                .message("CEP rules successfully reloaded")
                .ruleCount(cepRuleRegistry.getActiveRules().size())
                .build());
    }
}
