package com.notification.platform.worker;

import com.notification.platform.channel.ChannelGateway;
import com.notification.platform.channel.ChannelRouter;
import com.notification.platform.channel.model.DeliveryResult;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.NotificationStatus;
import com.notification.platform.domain.model.UserProfile;
import com.notification.platform.domain.repository.NotificationRepository;
import com.notification.platform.observability.NotificationMetrics;
import com.notification.platform.queue.NotificationQueue;
import com.notification.platform.resilience.DeadLetterQueueService;
import com.notification.platform.resilience.RetryPolicy;
import com.notification.platform.scheduler.NotificationSchedulerService;
import com.notification.platform.service.UserProfileEnrichmentService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWorker {

    private final NotificationQueue notificationQueue;
    private final NotificationRepository notificationRepository;
    private final ChannelRouter channelRouter;
    private final UserProfileEnrichmentService userProfileService;
    private final RetryPolicy retryPolicy;
    private final DeadLetterQueueService dlqService;
    private final NotificationSchedulerService schedulerService;
    private final NotificationMetrics metrics;

    @Value("${notification.queue.worker-threads:8}")
    private int workerThreads;

    private ExecutorService workerThreadPool;
    private volatile boolean running = true;

    @PostConstruct
    public void start() {
        workerThreadPool = Executors.newFixedThreadPool(workerThreads);
        log.info("Starting {} notification worker threads...", workerThreads);
        for (int i = 0; i < workerThreads; i++) {
            final int workerId = i + 1;
            workerThreadPool.submit(() -> runWorkerLoop(workerId));
        }
    }

    private void runWorkerLoop(int workerId) {
        log.info("Worker #{} initialized and polling for tasks.", workerId);
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Notification notification = notificationQueue.poll(500, TimeUnit.MILLISECONDS);
                if (notification != null) {
                    processNotification(notification, workerId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Worker #{} encountered unexpected error: {}", workerId, e.getMessage(), e);
            }
        }
        log.info("Worker #{} terminated.", workerId);
    }

    public void processNotification(Notification queuedItem, int workerId) {
        String traceId = "txn-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        long startTime = System.currentTimeMillis();

        try {
            log.info("[WORKER-{}] Processing notification '{}' for user '{}' via channel '{}' (Attempt: {})",
                    workerId, queuedItem.getId(), queuedItem.getUserId(), queuedItem.getChannel(), queuedItem.getRetryCount() + 1);

            Notification notification = notificationRepository.findById(queuedItem.getId())
                    .orElse(queuedItem);

            NotificationStatus previousStatus = notification.getStatus();
            notification.setStatus(NotificationStatus.PROCESSING);
            notification.addAuditLog(previousStatus, NotificationStatus.PROCESSING, "Worker #" + workerId + " picked up for delivery attempt " + (notification.getRetryCount() + 1));
            notificationRepository.save(notification);

            UserProfile profile = userProfileService.enrich(notification.getUserId(), notification.getCountry());
            ChannelGateway gateway = channelRouter.getGateway(notification.getChannel());

            DeliveryResult result = gateway.send(notification, profile);

            if (result.isSuccessful()) {
                handleDeliverySuccess(notification, result, startTime);
            } else {
                handleDeliveryFailure(notification, new RuntimeException(result.getErrorMessage()));
            }
        } catch (Throwable ex) {
            handleDeliveryFailure(queuedItem, ex);
        } finally {
            MDC.remove("traceId");
        }
    }

    private void handleDeliverySuccess(Notification notification, DeliveryResult result, long startTime) {
        Notification target = notificationRepository.findById(notification.getId()).orElse(notification);
        NotificationStatus previousStatus = target.getStatus();
        target.setStatus(NotificationStatus.SENT);
        target.setSentAt(Instant.now());
        target.setFailureReason(null);
        target.addAuditLog(previousStatus, NotificationStatus.SENT, "Delivered successfully. Ref: " + result.getGatewayReferenceId());
        notificationRepository.save(target);

        long durationMs = System.currentTimeMillis() - startTime;
        metrics.incrementDelivered(target.getChannel());
        metrics.recordDeliveryTime(target.getChannel(), Duration.ofMillis(durationMs));

        log.info("[DELIVERY-SUCCESS] Notification '{}' delivered via {} in {}ms. GatewayRef: {}",
                target.getId(), target.getChannel(), durationMs, result.getGatewayReferenceId());
    }

    private void handleDeliveryFailure(Notification notification, Throwable ex) {
        Notification target = notificationRepository.findById(notification.getId()).orElse(notification);
        boolean retryable = retryPolicy.isRetryable(ex);
        int currentAttempt = target.getRetryCount() + 1;
        target.setRetryCount(currentAttempt);

        log.warn("[DELIVERY-FAILED] Notification '{}' failed on attempt {}. Retryable: {}. Error: {}",
                target.getId(), currentAttempt, retryable, ex.getMessage());

        if (retryable && currentAttempt <= retryPolicy.getMaxAttempts()) {
            NotificationStatus previousStatus = target.getStatus();
            target.setStatus(NotificationStatus.RETRYING);
            target.setFailureReason(String.format("Attempt %d failed: %s", currentAttempt, ex.getMessage()));
            target.addAuditLog(previousStatus, NotificationStatus.RETRYING, "Attempt " + currentAttempt + " failed: " + ex.getMessage());
            notificationRepository.save(target);

            long backoffMs = retryPolicy.computeBackoffMs(currentAttempt);
            metrics.incrementRetrying(target.getChannel(), currentAttempt);

            log.info("[RETRY-TRIGGER] Notification '{}' scheduled for retry #{} in {}ms",
                    target.getId(), currentAttempt, backoffMs);
            schedulerService.scheduleRetry(target, backoffMs);
        } else {
            String terminalReason = (!retryable ? "NON_RETRYABLE_ERROR: " : "EXHAUSTED_MAX_RETRIES: ") + ex.getMessage();
            metrics.incrementFailed(target.getChannel(), ex.getClass().getSimpleName());
            dlqService.routeToDeadLetter(target, terminalReason);
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (workerThreadPool != null) {
            workerThreadPool.shutdown();
            try {
                if (!workerThreadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                    workerThreadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                workerThreadPool.shutdownNow();
            }
        }
    }
}
