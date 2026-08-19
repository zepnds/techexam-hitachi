package com.notification.platform.resilience.impl;

import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.NotificationStatus;
import com.notification.platform.domain.repository.NotificationRepository;
import com.notification.platform.observability.NotificationMetrics;
import com.notification.platform.resilience.DeadLetterQueueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeadLetterQueueServiceImpl implements DeadLetterQueueService {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationMetrics metrics;

    @Override
    @Transactional
    public void routeToDeadLetter(Notification notification, String terminalReason) {
        log.error("[DLQ-ALERT] Moving notification '{}' (User: {}, Channel: {}) to DEAD_LETTER queue. Terminal Reason: {}",
                notification.getId(), notification.getUserId(), notification.getChannel(), terminalReason);

        notification.setStatus(NotificationStatus.DEAD_LETTER);
        notification.setFailureReason("EXHAUSTED_RETRIES / UNRECOVERABLE: " + terminalReason);
        notificationRepository.save(notification);

        metrics.incrementDlq(notification.getChannel());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> getDeadLetterMessages(Pageable pageable) {
        return notificationRepository.findByStatus(NotificationStatus.DEAD_LETTER, pageable);
    }
}
