package com.notification.platform.observability;

import com.notification.platform.domain.model.ChannelType;
import com.notification.platform.queue.NotificationQueue;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class NotificationMetrics {

    private final MeterRegistry registry;

    public NotificationMetrics(MeterRegistry registry, NotificationQueue queue) {
        this.registry = registry;
        registry.gauge("notification_queue_size", queue, NotificationQueue::size);
    }

    public void incrementReceived(ChannelType channel) {
        registry.counter("notifications_received_total", "channel", channel.name()).increment();
    }

    public void incrementDelivered(ChannelType channel) {
        registry.counter("notifications_delivered_total", "channel", channel.name()).increment();
    }

    public void incrementFailed(ChannelType channel, String reason) {
        registry.counter("notifications_failed_total", "channel", channel.name(), "reason", reason).increment();
    }

    public void incrementRetrying(ChannelType channel, int attempt) {
        registry.counter("notifications_retrying_total", "channel", channel.name(), "attempt", String.valueOf(attempt)).increment();
    }

    public void incrementCepDropped(String ruleId) {
        registry.counter("notifications_cep_dropped_total", "rule", ruleId != null ? ruleId : "UNKNOWN").increment();
    }

    public void incrementCepDelayed(String ruleId) {
        registry.counter("notifications_cep_delayed_total", "rule", ruleId != null ? ruleId : "UNKNOWN").increment();
    }

    public void incrementDlq(ChannelType channel) {
        registry.counter("notifications_dlq_total", "channel", channel.name()).increment();
    }

    public void recordDeliveryTime(ChannelType channel, Duration duration) {
        Timer.builder("notifications_delivery_duration_seconds")
                .tag("channel", channel.name())
                .register(registry)
                .record(duration);
    }
}
