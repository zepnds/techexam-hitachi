package com.notification.platform.scheduler.impl;

import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.NotificationStatus;
import com.notification.platform.domain.repository.NotificationRepository;
import com.notification.platform.queue.NotificationQueue;
import com.notification.platform.scheduler.NotificationSchedulerService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class NotificationSchedulerServiceImpl implements NotificationSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSchedulerServiceImpl.class);

    private final NotificationQueue notificationQueue;
    private final NotificationRepository notificationRepository;
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(4);

    @Override
    public void scheduleDelayedDelivery(Notification notification, Instant targetDeliveryTime) {
        long delayMs = Math.max(0, targetDeliveryTime.toEpochMilli() - Instant.now().toEpochMilli());

        log.info("[SCHEDULER] Scheduling notification '{}' for execution in {}ms (at {})",
                notification.getId(), delayMs, targetDeliveryTime);

        scheduledExecutor.schedule(() -> {
            try {
                log.info("[SCHEDULER] Triggering scheduled notification: {}", notification.getId());
                notification.setStatus(NotificationStatus.QUEUED);
                notificationRepository.save(notification);
                notificationQueue.enqueue(notification);
            } catch (Exception e) {
                log.error("[SCHEDULER-ERROR] Failed to enqueue scheduled notification {}: {}",
                        notification.getId(), e.getMessage(), e);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void scheduleRetry(Notification notification, long delayMs) {
        log.info("[RETRY-SCHEDULE] Scheduling retry #{} for notification '{}' in {}ms",
                notification.getRetryCount(), notification.getId(), delayMs);

        scheduledExecutor.schedule(() -> {
            try {
                notificationQueue.enqueue(notification);
            } catch (Exception e) {
                log.error("[RETRY-SCHEDULE-ERROR] Failed to enqueue retry for notification {}: {}",
                        notification.getId(), e.getMessage(), e);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    @Override
    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void pollDueScheduledNotifications() {
        Instant now = Instant.now();
        List<Notification> dueNotifications = notificationRepository.findDueScheduledNotifications(NotificationStatus.DELAYED, now);

        if (!dueNotifications.isEmpty()) {
            log.info("[SCHEDULER-POLLER] Found {} due scheduled notifications to enqueue.", dueNotifications.size());
            for (Notification n : dueNotifications) {
                n.setStatus(NotificationStatus.QUEUED);
                notificationRepository.save(n);
                notificationQueue.enqueue(n);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduledExecutor.shutdown();
    }
}
