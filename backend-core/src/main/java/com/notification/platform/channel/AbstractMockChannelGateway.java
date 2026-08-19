package com.notification.platform.channel;

import com.notification.platform.channel.exception.PermanentDeliveryException;
import com.notification.platform.channel.exception.TransientDeliveryException;
import com.notification.platform.channel.model.DeliveryResult;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractMockChannelGateway implements ChannelGateway {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Value("${notification.gateways.simulate-latency-ms:30}")
    private long baseLatencyMs;

    @Value("${notification.gateways.failure-simulation.enabled:false}")
    private boolean failureSimulationEnabled;

    @Value("${notification.gateways.failure-simulation.transient-failure-rate:0.10}")
    private double transientFailureRate;

    @Value("${notification.gateways.failure-simulation.permanent-failure-rate:0.02}")
    private double permanentFailureRate;

   
    private volatile boolean forceSuccess = false;
    private volatile boolean forceTransientFailure = false;
    private volatile boolean forcePermanentFailure = false;

    public void setForceSuccess(boolean force) { this.forceSuccess = force; }
    public void setForceTransientFailure(boolean force) { this.forceTransientFailure = force; }
    public void setForcePermanentFailure(boolean force) { this.forcePermanentFailure = force; }

    @Override
    public DeliveryResult send(Notification notification, UserProfile profile) {
        long startTime = System.currentTimeMillis();
        String channelName = getChannelType().name();
        String destination = resolveDestination(notification, profile);

        log.info("[GATEWAY-DISPATCH] [{}] Sending notification '{}' to destination: '{}' (User: {})",
                channelName, notification.getId(), destination, notification.getUserId());

        simulateNetworkLatency();

        if (forcePermanentFailure) {
            log.error("[GATEWAY-ERROR] [{}] Forced permanent failure for notification: {}", channelName, notification.getId());
            throw new PermanentDeliveryException("Simulated permanent gateway failure (Invalid recipient / Unreachable account)");
        }

        if (forceTransientFailure) {
            log.warn("[GATEWAY-WARN] [{}] Forced transient failure for notification: {}", channelName, notification.getId());
            throw new TransientDeliveryException("Simulated transient gateway error (HTTP 503 Provider Service Unavailable)");
        }

        if (failureSimulationEnabled && !forceSuccess) {
            double randomVal = ThreadLocalRandom.current().nextDouble();
            if (randomVal < permanentFailureRate) {
                log.error("[GATEWAY-ERROR] [{}] Simulated permanent failure for notification: {}", channelName, notification.getId());
                throw new PermanentDeliveryException("Downstream carrier returned permanent error 400 Bad Request");
            } else if (randomVal < (permanentFailureRate + transientFailureRate)) {
                log.warn("[GATEWAY-WARN] [{}] Simulated transient failure for notification: {}", channelName, notification.getId());
                throw new TransientDeliveryException("Downstream carrier timeout / 503 Service Unavailable");
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        String gatewayRef = channelName.toLowerCase() + "_gw_" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[GATEWAY-SUCCESS] [{}] Notification '{}' delivered successfully in {}ms (Ref: {})",
                channelName, notification.getId(), elapsed, gatewayRef);

        return DeliveryResult.success(gatewayRef, elapsed);
    }

    protected abstract String resolveDestination(Notification notification, UserProfile profile);

    private void simulateNetworkLatency() {
        try {
            long jitter = ThreadLocalRandom.current().nextLong(5, 25);
            Thread.sleep(Math.max(1, baseLatencyMs + jitter));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
