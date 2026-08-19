package com.notification.platform.service;

import com.notification.common.dto.SendNotificationRequest;
import com.notification.common.model.ChannelType;
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
import com.notification.platform.service.impl.NotificationOrchestratorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationOrchestratorTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserProfileEnrichmentService enrichmentService;
    @Mock
    private CepEngine cepEngine;
    @Mock
    private NotificationQueue notificationQueue;
    @Mock
    private NotificationSchedulerService schedulerService;
    @Mock
    private NotificationMetrics metrics;
    @Mock
    private NatsMessagingService natsService;

    @Spy
    private NotificationMapper notificationMapper = new NotificationMapper();

    private NotificationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new NotificationOrchestratorImpl(
                notificationRepository,
                notificationQueue,
                schedulerService,
                metrics,
                notificationMapper,
                natsService,
                enrichmentService,
                cepEngine
        );

        lenient().when(enrichmentService.enrich(anyString(), any())).thenReturn(
                UserProfile.builder().userId("user123").country("PHILIPPINES").build()
        );
        lenient().when(notificationRepository.saveAndFlush(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should enqueue notification when all CEP rules PASS")
    void testSuccessfulPassAndEnqueue() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId("user123")
                .channel(ChannelType.EMAIL)
                .message("Welcome to our service")
                .build();

        when(cepEngine.evaluate(any(), any(), any())).thenReturn(CepEvaluationResult.pass());
        when(notificationQueue.enqueue(any())).thenReturn(true);

        Notification result = orchestrator.processNotificationRequest(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.QUEUED);
        verify(notificationQueue).enqueue(result);
        verify(metrics).incrementReceived(com.notification.platform.domain.model.ChannelType.EMAIL);
    }

    @Test
    @DisplayName("Should mark DROPPED and NOT enqueue when CEP rule drops the event")
    void testCepRuleDrop() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId("user123")
                .channel(ChannelType.SMS)
                .message("Spam text")
                .build();

        when(cepEngine.evaluate(any(), any(), any())).thenReturn(
                CepEvaluationResult.drop("RULE-RATE-LIMIT-SMS-002", "Rate Limit", "Max 3 SMS per hour")
        );

        Notification result = orchestrator.processNotificationRequest(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.DROPPED);
        assertThat(result.getDropReason()).isEqualTo("Max 3 SMS per hour");
        assertThat(result.getAppliedCepRule()).isEqualTo("RULE-RATE-LIMIT-SMS-002");
        verify(notificationQueue, never()).enqueue(any());
        verify(metrics).incrementCepDropped("RULE-RATE-LIMIT-SMS-002");
    }

    @Test
    @DisplayName("Should mark DELAYED and schedule when CEP rule delays the event")
    void testCepRuleDelay() {
        Instant delayUntil = Instant.now().plusSeconds(3600);
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId("user123")
                .channel(ChannelType.SMS)
                .message("Late night promo")
                .build();

        when(cepEngine.evaluate(any(), any(), any())).thenReturn(
                CepEvaluationResult.delay("RULE-QUIET-HOURS-PH-003", "Quiet Hours", "Delayed until 06:00 AM", delayUntil)
        );

        Notification result = orchestrator.processNotificationRequest(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.DELAYED);
        assertThat(result.getScheduleAt()).isEqualTo(delayUntil);
        verify(schedulerService).scheduleDelayedDelivery(eq(result), eq(delayUntil));
        verify(notificationQueue, never()).enqueue(any());
    }
}
