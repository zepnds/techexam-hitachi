package com.notification.platform.resilience;

import com.notification.platform.channel.exception.ChannelDeliveryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RetryPolicy {

    private final int maxAttempts;
    private final long initialIntervalMs;
    private final double multiplier;
    private final long maxIntervalMs;
    private final boolean jitterEnabled;

    public RetryPolicy(
            @Value("${notification.retry.max-attempts:3}") int maxAttempts,
            @Value("${notification.retry.initial-interval-ms:1000}") long initialIntervalMs,
            @Value("${notification.retry.multiplier:2.0}") double multiplier,
            @Value("${notification.retry.max-interval-ms:10000}") long maxIntervalMs,
            @Value("${notification.retry.jitter:true}") boolean jitterEnabled) {
        this.maxAttempts = maxAttempts;
        this.initialIntervalMs = initialIntervalMs;
        this.multiplier = multiplier;
        this.maxIntervalMs = maxIntervalMs;
        this.jitterEnabled = jitterEnabled;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

   
    public long computeBackoffMs(int attempt) {
        if (attempt <= 0) {
            return 0;
        }
        double rawBackoff = initialIntervalMs * Math.pow(multiplier, attempt - 1);
        long cappedBackoff = Math.min((long) rawBackoff, maxIntervalMs);

        if (jitterEnabled) {
        
            long min = (long) (cappedBackoff * 0.5);
            long max = (long) (cappedBackoff * 1.5);
            return ThreadLocalRandom.current().nextLong(Math.max(1, min), Math.max(min + 1, max));
        }

        return cappedBackoff;
    }


    public boolean isRetryable(Throwable throwable) {
        if (throwable instanceof ChannelDeliveryException channelException) {
            return channelException.isRetryable();
        }
        
        return (throwable instanceof java.io.IOException ||
                throwable instanceof java.util.concurrent.TimeoutException ||
                throwable instanceof java.net.SocketTimeoutException);
    }
}
