package com.notification.platform.nats.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.nats.config.NatsProperties;
import com.notification.platform.nats.service.NatsMessagingService;
import io.nats.client.*;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NatsMessagingServiceImpl implements NatsMessagingService {

    private final NatsProperties natsProperties;
    private final ObjectMapper objectMapper;

    private Connection natsConnection;
    private JetStream jetStream;

    @PostConstruct
    public void init() {
        if (!natsProperties.isEnabled()) {
            log.info("NATS messaging disabled by configuration.");
            return;
        }

        try {
            log.info("Connecting to NATS Server at '{}'...", natsProperties.getServerUrl());
            Options options = new Options.Builder()
                    .server(natsProperties.getServerUrl())
                    .connectionTimeout(java.time.Duration.ofSeconds(5))
                    .build();
            natsConnection = Nats.connect(options);
            jetStream = natsConnection.jetStream();

    
            JetStreamManagement jsm = natsConnection.jetStreamManagement();
            String streamName = natsProperties.getStreamName();
            String subjectMatch = natsProperties.getSubjectPrefix() + ".>";

            try {
                jsm.getStreamInfo(streamName);
                log.info("NATS JetStream Stream '{}' already exists.", streamName);
            } catch (JetStreamApiException e) {
                if (e.getErrorCode() == 404) {
                    StreamConfiguration streamConfig = StreamConfiguration.builder()
                            .name(streamName)
                            .subjects(subjectMatch, natsProperties.getDlqSubject())
                            .storageType(StorageType.File)
                            .build();
                    jsm.addStream(streamConfig);
                    log.info("Created NATS JetStream Stream '{}' matching subjects '{}'", streamName, subjectMatch);
                }
            }

            log.info("NATS JetStream Connection established successfully.");
        } catch (Exception e) {
            log.warn("Failed to connect to NATS Server at '{}': {}. Falling back to internal queue.",
                    natsProperties.getServerUrl(), e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        return natsConnection != null && natsConnection.getStatus() == Connection.Status.CONNECTED;
    }

    @Override
    public void publishNotificationEvent(Notification notification) {
        if (!isConnected()) {
            log.debug("NATS not connected. Skipping NATS publish for notification: {}", notification.getId());
            return;
        }

        try {
            String subject = natsProperties.getSubjectPrefix() + "." + notification.getChannel().name();
            String payload = objectMapper.writeValueAsString(Map.of(
                    "id", notification.getId(),
                    "userId", notification.getUserId(),
                    "channel", notification.getChannel().name(),
                    "status", notification.getStatus().name()
            ));

            jetStream.publish(subject, payload.getBytes(StandardCharsets.UTF_8));
            log.info("[NATS-PUBLISH] Notification '{}' published to subject '{}'", notification.getId(), subject);
        } catch (Exception e) {
            log.error("Failed to publish NATS message for notification {}: {}", notification.getId(), e.getMessage(), e);
        }
    }

    @Override
    public void publishDlqEvent(Notification notification, String terminalReason) {
        if (!isConnected()) {
            return;
        }

        try {
            String subject = natsProperties.getDlqSubject();
            String payload = objectMapper.writeValueAsString(Map.of(
                    "id", notification.getId(),
                    "userId", notification.getUserId(),
                    "channel", notification.getChannel().name(),
                    "terminalReason", terminalReason
            ));

            jetStream.publish(subject, payload.getBytes(StandardCharsets.UTF_8));
            log.warn("[NATS-DLQ-PUBLISH] DLQ Notification '{}' published to subject '{}'", notification.getId(), subject);
        } catch (Exception e) {
            log.error("Failed to publish NATS DLQ message for notification {}: {}", notification.getId(), e.getMessage(), e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (natsConnection != null) {
            try {
                natsConnection.close();
                log.info("NATS Connection closed.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
