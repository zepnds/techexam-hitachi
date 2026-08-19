package com.notification.platform.worker;

import com.notification.platform.channel.ChannelRouter;
import com.notification.platform.channel.exception.PermanentDeliveryException;
import com.notification.platform.channel.exception.TransientDeliveryException;
import com.notification.platform.channel.impl.EmailGatewayService;
import com.notification.platform.domain.model.ChannelType;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.NotificationStatus;
import com.notification.platform.domain.repository.NotificationRepository;
import com.notification.platform.observability.NotificationMetrics;
import com.notification.platform.queue.NotificationQueue;
import com.notification.platform.resilience.DeadLetterQueueService;
import com.notification.platform.resilience.RetryPolicy;
import com.notification.platform.scheduler.NotificationSchedulerService;
import com.notification.platform.service.UserProfileEnrichmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationWorkerTest {

    @Mock
    private NotificationQueue notificationQueue;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private ChannelRouter channelRouter;
    @Mock
    private UserProfileEnrichmentService userProfileService;
    @Mock
    private DeadLetterQueueService dlqService;
    @Mock
    private NotificationSchedulerService schedulerService;
    @Mock
    private NotificationMetrics metrics;
    @Mock
    private EmailGatewayService emailGateway;

    private RetryPolicy retryPolicy;
    private NotificationWorker worker;

    @BeforeEach
    void setUp() {
        retryPolicy = new RetryPolicy(3, 1000L, 2.0, 10000L, false);
        worker = new NotificationWorker(
                notificationQueue,
                notificationRepository,
                channelRouter,
                userProfileService,
                retryPolicy,
                dlqService,
                schedulerService,
                metrics
        );
    }

    @Test
    @DisplayName("Should schedule retry when a transient delivery error occurs")
    void testTransientFailureTriggersRetry() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .userId("retry_user")
                .channel(ChannelType.EMAIL)
                .message("Test retry message")
                .status(NotificationStatus.QUEUED)
                .retryCount(0)
                .build();

        when(channelRouter.getGateway(ChannelType.EMAIL)).thenReturn(emailGateway);
        when(emailGateway.send(any(), any())).thenThrow(new TransientDeliveryException("503 Service Unavailable"));

        worker.processNotification(notification, 1);

        verify(schedulerService).scheduleRetry(eq(notification), anyLong());
        verify(dlqService, never()).routeToDeadLetter(any(), any());
    }

    @Test
    @DisplayName("Should immediately route to DLQ when a permanent delivery error occurs")
    void testPermanentFailureRoutesToDlq() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .userId("invalid_user")
                .channel(ChannelType.EMAIL)
                .message("Test permanent error")
                .status(NotificationStatus.QUEUED)
                .retryCount(0)
                .build();

        when(channelRouter.getGateway(ChannelType.EMAIL)).thenReturn(emailGateway);
        when(emailGateway.send(any(), any())).thenThrow(new PermanentDeliveryException("Invalid email address syntax"));

        worker.processNotification(notification, 1);

        verify(dlqService).routeToDeadLetter(eq(notification), contains("NON_RETRYABLE_ERROR"));
        verify(schedulerService, never()).scheduleRetry(any(), anyLong());
    }

    @Test
    @DisplayName("Should route to DLQ when max retry attempts are exhausted")
    void testExhaustedRetriesRoutesToDlq() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .userId("exhausted_user")
                .channel(ChannelType.EMAIL)
                .message("Test exhausted retries")
                .status(NotificationStatus.RETRYING)
                .retryCount(3) // Already attempted 3 times
                .build();

        when(channelRouter.getGateway(ChannelType.EMAIL)).thenReturn(emailGateway);
        when(emailGateway.send(any(), any())).thenThrow(new TransientDeliveryException("503 Gateway Timeout"));

        worker.processNotification(notification, 1);

        verify(dlqService).routeToDeadLetter(eq(notification), contains("EXHAUSTED_MAX_RETRIES"));
        verify(schedulerService, never()).scheduleRetry(any(), anyLong());
    }
}
