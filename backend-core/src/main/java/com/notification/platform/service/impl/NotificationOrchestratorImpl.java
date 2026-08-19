package com.notification.platform.service.impl;

import com.notification.common.dto.SendNotificationRequest;
import com.notification.platform.cep.model.CepAction;
import com.notification.platform.cep.model.CepEvaluationResult;
import com.notification.platform.cep.service.CepEngine;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.NotificationStatus;
import com.notification.platform.domain.model.UserProfile;
import com.notification.platform.domain.repository.NotificationRepository;
import com.notification.platform.dto.mapper.NotificationMapper;
import com.notification.platform.nats.service.NatsMessagingService;
import com.notification.platform.observability.NotificationMetrics;
import com.notification.platform.queue.NotificationQueue;
import com.notification.platform.scheduler.NotificationSchedulerService;
import com.notification.platform.service.NotificationOrchestrator;
import com.notification.platform.service.UserProfileEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOrchestratorImpl implements NotificationOrchestrator {

    private final NotificationRepository notificationRepository;
    private final NotificationQueue notificationQueue;
    private final NotificationSchedulerService schedulerService;
    private final NotificationMetrics metrics;
    private final NotificationMapper notificationMapper;
    private final NatsMessagingService natsService;
    private final UserProfileEnrichmentService enrichmentService;
    private final CepEngine cepEngine;

    @Override
    public Notification processNotificationRequest(SendNotificationRequest request) {
        Instant now = Instant.now();
        Notification notification = notificationMapper.toEntity(request);

        metrics.incrementReceived(notification.getChannel());
        notification.addAuditLog(null, NotificationStatus.QUEUED, "Notification request accepted by API ingress");

        // 1. Handle Explicit Client Future Scheduling
        if (request.getScheduleAt() != null && request.getScheduleAt().isAfter(now)) {
            log.info("Client requested future schedule at {} for notification {}", request.getScheduleAt(), notification.getId());
            notification.setStatus(NotificationStatus.DELAYED);
            notification.addAuditLog(NotificationStatus.QUEUED, NotificationStatus.DELAYED, "Scheduled for future delivery at " + request.getScheduleAt());
            Notification saved = notificationRepository.saveAndFlush(notification);
            schedulerService.scheduleDelayedDelivery(saved, request.getScheduleAt());
            return saved;
        }

        // 2. User Profile Context Enrichment
        UserProfile profile = enrichmentService.enrich(notification.getUserId(), notification.getCountry());
        if (notification.getCountry() == null && profile.getCountry() != null) {
            notification.setCountry(profile.getCountry());
        }

        // 3. CEP Rules Pipeline Evaluation
        CepEvaluationResult cepResult = cepEngine.evaluate(notification, profile, now);
        
        if (cepResult.getAction() == CepAction.DROP) {
            notification.setStatus(NotificationStatus.DROPPED);
            notification.setDropReason(cepResult.getReason());
            notification.setAppliedCepRule(cepResult.getTriggeredRuleId());
            notification.addAuditLog(NotificationStatus.QUEUED, NotificationStatus.DROPPED, "Suppressed by CEP rule: " + cepResult.getTriggeredRuleId());
            metrics.incrementCepDropped(cepResult.getTriggeredRuleId());
            log.warn("Notification {} DROPPED by CEP rule '{}'", notification.getId(), cepResult.getTriggeredRuleId());
            return notificationRepository.saveAndFlush(notification);
        }

        if (cepResult.getAction() == CepAction.DELAY) {
            notification.setStatus(NotificationStatus.DELAYED);
            notification.setScheduleAt(cepResult.getDelayUntil());
            notification.setAppliedCepRule(cepResult.getTriggeredRuleId());
            notification.addAuditLog(NotificationStatus.QUEUED, NotificationStatus.DELAYED, "Delayed by CEP rule: " + cepResult.getTriggeredRuleId());
            metrics.incrementCepDelayed(cepResult.getTriggeredRuleId());
            log.info("Notification {} DELAYED by CEP rule '{}'", notification.getId(), cepResult.getTriggeredRuleId());
            schedulerService.scheduleDelayedDelivery(notification, notification.getScheduleAt());
            return notificationRepository.saveAndFlush(notification);
        }

        if (cepResult.getAction() == CepAction.REROUTE && cepResult.getReroutedChannel() != null) {
            notification.setChannel(cepResult.getReroutedChannel());
            notification.setAppliedCepRule(cepResult.getTriggeredRuleId());
            notification.addAuditLog(NotificationStatus.QUEUED, NotificationStatus.QUEUED, "Rerouted to channel: " + cepResult.getReroutedChannel());
        }

        // 4. Queue / NATS Event Streaming Dispatch
        if (notification.getStatus() == NotificationStatus.QUEUED) {
            Notification savedNotification = notificationRepository.saveAndFlush(notification);
            
            if (natsService.isConnected()) {
                natsService.publishNotificationEvent(savedNotification);
            }
            
            boolean enqueued = notificationQueue.enqueue(savedNotification);
            if (!enqueued && !natsService.isConnected()) {
                savedNotification.setStatus(NotificationStatus.FAILED);
                savedNotification.setFailureReason("Queue capacity exceeded / Ingress throttling");
                savedNotification.addAuditLog(NotificationStatus.QUEUED, NotificationStatus.FAILED, "Queue capacity exceeded");
                return notificationRepository.saveAndFlush(savedNotification);
            }
            return savedNotification;
        }

        return notificationRepository.saveAndFlush(notification);
    }
}
