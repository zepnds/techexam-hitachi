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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class DeduplicationRuleEvaluator implements CepRuleEvaluator {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationRuleEvaluator.class);
    private final WindowStateStore windowStateStore;

    @Override
    public CepRuleType supportsType() {
        return CepRuleType.DEDUPLICATION;
    }

    @Override
    public CepEvaluationResult evaluate(Notification notification, UserProfile profile, CepRuleDefinition rule, Instant now) {
        String userId = notification.getUserId();
        String message = notification.getMessage() != null ? notification.getMessage().trim() : "";
        String messageHash = hashString(message);

        String stateKey = "dedup:" + userId + ":" + messageHash;
        long windowSecs = rule.getWindowSeconds() != null ? rule.getWindowSeconds() : 300; 
        Duration windowDuration = Duration.ofSeconds(windowSecs);

        boolean isDuplicate = windowStateStore.checkAndRecordDuplicate(stateKey, windowDuration, now);
        if (isDuplicate) {
            String reason = rule.getReason() != null ? rule.getReason() :
                    String.format("Duplicate message suppressed for user '%s' within %ds deduplication window", userId, windowSecs);
            log.warn("CEP [DEDUPLICATION] Rule '{}' triggered for user {}: {}", rule.getId(), userId, reason);
            return CepEvaluationResult.drop(rule.getId(), rule.getName(), reason);
        }

        return CepEvaluationResult.pass();
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
