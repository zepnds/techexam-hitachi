package com.notification.platform.resilience;

import com.notification.platform.channel.exception.PermanentDeliveryException;
import com.notification.platform.channel.exception.TransientDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class RetryPolicyTest {

    private RetryPolicy retryPolicyWithJitter;
    private RetryPolicy retryPolicyDeterministic;

    @BeforeEach
    void setUp() {
        // With jitter
        retryPolicyWithJitter = new RetryPolicy(3, 1000L, 2.0, 10000L, true);
        // Deterministic without jitter
        retryPolicyDeterministic = new RetryPolicy(3, 1000L, 2.0, 10000L, false);
    }

    @Test
    @DisplayName("Should compute exponentially increasing backoff intervals")
    void testExponentialBackoffIntervals() {
        // Attempt 1: 1000 * 2^0 = 1000ms
        assertThat(retryPolicyDeterministic.computeBackoffMs(1)).isEqualTo(1000L);

        // Attempt 2: 1000 * 2^1 = 2000ms
        assertThat(retryPolicyDeterministic.computeBackoffMs(2)).isEqualTo(2000L);

        // Attempt 3: 1000 * 2^2 = 4000ms
        assertThat(retryPolicyDeterministic.computeBackoffMs(3)).isEqualTo(4000L);

        // Attempt 4: 1000 * 2^3 = 8000ms
        assertThat(retryPolicyDeterministic.computeBackoffMs(4)).isEqualTo(8000L);

        // Attempt 5: 1000 * 2^4 = 16000ms -> capped at max 10000ms
        assertThat(retryPolicyDeterministic.computeBackoffMs(5)).isEqualTo(10000L);
    }

    @Test
    @DisplayName("Should calculate jittered backoff within [0.5 * backoff, 1.5 * backoff]")
    void testJitterBounds() {
        for (int i = 0; i < 20; i++) {
            long backoff = retryPolicyWithJitter.computeBackoffMs(1);
            assertThat(backoff).isBetween(500L, 1500L);
        }
    }

    @Test
    @DisplayName("Should correctly classify retryable and permanent exceptions")
    void testExceptionClassification() {
        assertThat(retryPolicyDeterministic.isRetryable(new TransientDeliveryException("503 Gateway Timeout"))).isTrue();
        assertThat(retryPolicyDeterministic.isRetryable(new SocketTimeoutException("Connection timed out"))).isTrue();
        assertThat(retryPolicyDeterministic.isRetryable(new IOException("Broken pipe"))).isTrue();

        assertThat(retryPolicyDeterministic.isRetryable(new PermanentDeliveryException("400 Invalid Recipient Phone"))).isFalse();
        assertThat(retryPolicyDeterministic.isRetryable(new IllegalArgumentException("Invalid payload"))).isFalse();
    }
}
