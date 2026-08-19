package com.notification.platform.cep.evaluator;

import com.notification.platform.cep.model.CepEvaluationResult;
import com.notification.platform.cep.model.CepRuleDefinition;
import com.notification.platform.cep.model.CepRuleType;
import com.notification.platform.cep.state.WindowStateStore;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RateLimitRuleEvaluator implements CepRuleEvaluator {

    private static final Logger log = LoggerFactory.getLogger(RateLimitRuleEvaluator.class);
    private final WindowStateStore windowStateStore;

    @Override
    public CepRuleType supportsType() {
        return CepRuleType.RATE_LIMIT;
    }

    @Override
    public CepEvaluationResult evaluate(Notification notification, UserProfile profile, CepRuleDefinition rule, Instant now) {

        if (rule.getTargetChannel() != null && rule.getTargetChannel() != notification.getChannel()) {
            return CepEvaluationResult.pass();
        }

        String userId = notification.getUserId();
        String channel = notification.getChannel().name();
        String stateKey = "rate:" + userId + ":" + channel;

        long windowSecs = rule.getWindowSeconds() != null ? rule.getWindowSeconds() : 3600;
        int maxAllowed = rule.getMaxAllowedEvents() != null ? rule.getMaxAllowedEvents() : 3;

        Duration windowDuration = Duration.ofSeconds(windowSecs);
        int currentCount = windowStateStore.countEventsInWindow(stateKey, windowDuration, now);

        if (currentCount >= maxAllowed) {
            String reason = rule.getReason() != null ? rule.getReason() :
                    String.format("Rate limit exceeded: user '%s' sent %d messages on channel %s within %ds (limit: %d)",
                            userId, currentCount, channel, windowSecs, maxAllowed);
            log.warn("CEP [RATE_LIMIT] Rule '{}' triggered for user {}: {}", rule.getId(), userId, reason);
            return CepEvaluationResult.drop(rule.getId(), rule.getName(), reason);
        }

       
        windowStateStore.recordEvent(stateKey, now);
        return CepEvaluationResult.pass();
    }
}
