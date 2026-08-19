package com.notification.platform.channel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResult {
    private boolean successful;
    private String gatewayReferenceId;
    private long latencyMs;
    private Instant deliveredAt;
    private String errorMessage;
    private boolean retryable;

    public static DeliveryResult success(String gatewayRef, long latencyMs) {
        return DeliveryResult.builder()
                .successful(true)
                .gatewayReferenceId(gatewayRef)
                .latencyMs(latencyMs)
                .deliveredAt(Instant.now())
                .build();
    }

    public static DeliveryResult failure(String errorMessage, boolean retryable, long latencyMs) {
        return DeliveryResult.builder()
                .successful(false)
                .errorMessage(errorMessage)
                .retryable(retryable)
                .latencyMs(latencyMs)
                .build();
    }
}
