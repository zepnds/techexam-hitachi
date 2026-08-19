package com.notification.platform.nats.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "notification.nats")
public class NatsProperties {

    private boolean enabled = false;
    private String serverUrl = "nats://localhost:4222";
    private String subjectPrefix = "notification.events";
    private String dlqSubject = "notification.dlq.events";
    private String streamName = "NOTIFICATION_STREAM";
    private String queueGroup = "notification-workers";
}
