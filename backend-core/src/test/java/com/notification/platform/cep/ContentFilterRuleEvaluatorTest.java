package com.notification.platform.cep;

import com.notification.platform.cep.evaluator.ContentFilterRuleEvaluator;
import com.notification.platform.cep.model.CepAction;
import com.notification.platform.cep.model.CepEvaluationResult;
import com.notification.platform.cep.model.CepRuleDefinition;
import com.notification.platform.cep.model.CepRuleType;
import com.notification.platform.domain.model.ChannelType;
import com.notification.platform.domain.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContentFilterRuleEvaluatorTest {

    private ContentFilterRuleEvaluator evaluator;
    private CepRuleDefinition filterRule;

    @BeforeEach
    void setUp() {
        evaluator = new ContentFilterRuleEvaluator();

        filterRule = CepRuleDefinition.builder()
                .id("RULE-CONTENT-FILTER-004")
                .name("Security Filter")
                .ruleType(CepRuleType.CONTENT_FILTER)
                .enabled(true)
                .action(CepAction.DROP)
                .conditions(Map.of("blacklistedKeywords", List.of("FREE_CRYPTO_SCAM", "PHISHING_TEST_TOKEN")))
                .reason("Prohibited spam keyword")
                .build();
    }

    @Test
    @DisplayName("Should DROP message containing blacklisted keyword")
    void testDropBlacklistedContent() {
        Notification notif = Notification.builder()
                .userId("victim1")
                .channel(ChannelType.SMS)
                .message("Claim your prize now with FREE_CRYPTO_SCAM link!")
                .build();

        CepEvaluationResult result = evaluator.evaluate(notif, null, filterRule, Instant.now());
        assertThat(result.getAction()).isEqualTo(CepAction.DROP);
        assertThat(result.getTriggeredRuleId()).isEqualTo("RULE-CONTENT-FILTER-004");
    }

    @Test
    @DisplayName("Should PASS legitimate message")
    void testPassCleanContent() {
        Notification notif = Notification.builder()
                .userId("user1")
                .channel(ChannelType.EMAIL)
                .message("Your monthly statement is ready for review.")
                .build();

        CepEvaluationResult result = evaluator.evaluate(notif, null, filterRule, Instant.now());
        assertThat(result.getAction()).isEqualTo(CepAction.PASS);
    }
}
