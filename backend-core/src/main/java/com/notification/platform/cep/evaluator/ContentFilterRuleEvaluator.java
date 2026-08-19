package com.notification.platform.cep.evaluator;

import com.notification.platform.cep.model.CepEvaluationResult;
import com.notification.platform.cep.model.CepRuleDefinition;
import com.notification.platform.cep.model.CepRuleType;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

@Slf4j
@Component
public class ContentFilterRuleEvaluator implements CepRuleEvaluator {

    @Override
    public CepRuleType supportsType() {
        return CepRuleType.CONTENT_FILTER;
    }

    @Override
    public CepEvaluationResult evaluate(Notification notification, UserProfile profile, CepRuleDefinition rule, Instant now) {
        if (notification.getMessage() == null) {
            return CepEvaluationResult.pass();
        }

        Map<String, Object> conditions = rule.getConditions();
        if (conditions == null || !conditions.containsKey("blacklistedKeywords")) {
            return CepEvaluationResult.pass();
        }

        Object rawKeywords = conditions.get("blacklistedKeywords");
        if (!(rawKeywords instanceof Collection<?> collection)) {
            return CepEvaluationResult.pass();
        }

        String messageUpper = notification.getMessage().toUpperCase();

        for (Object item : collection) {
            if (item != null) {
                String keyword = item.toString().trim();
                if (!keyword.isEmpty() && messageUpper.contains(keyword.toUpperCase())) {
                    String reason = rule.getReason() != null ? rule.getReason() :
                            "Message contains prohibited keyword: " + keyword;
                    log.warn("CEP [CONTENT_FILTER] Rule '{}' triggered for user {}: {}", rule.getId(), notification.getUserId(), reason);
                    return CepEvaluationResult.drop(rule.getId(), rule.getName(), reason);
                }
            }
        }

        return CepEvaluationResult.pass();
    }
}
