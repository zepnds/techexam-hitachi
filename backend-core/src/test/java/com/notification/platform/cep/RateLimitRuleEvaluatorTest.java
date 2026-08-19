package com.notification.platform.cep;

import com.notification.platform.cep.evaluator.RateLimitRuleEvaluator;
import com.notification.platform.cep.model.CepAction;
import com.notification.platform.cep.model.CepEvaluationResult;
import com.notification.platform.cep.model.CepRuleDefinition;
import com.notification.platform.cep.model.CepRuleType;
import com.notification.platform.cep.state.InMemoryWindowStateStore;
import com.notification.platform.domain.model.ChannelType;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitRuleEvaluatorTest {

    private InMemoryWindowStateStore stateStore;
    private RateLimitRuleEvaluator evaluator;
    private CepRuleDefinition rateLimitRule;

    @BeforeEach
    void setUp() {
        stateStore = new InMemoryWindowStateStore();
        evaluator = new RateLimitRuleEvaluator(stateStore);

        rateLimitRule = CepRuleDefinition.builder()
                .id("RULE-RATE-LIMIT-SMS-002")
                .name("SMS Channel Hourly Rate Limit")
                .ruleType(CepRuleType.RATE_LIMIT)
                .enabled(true)
                .action(CepAction.DROP)
                .windowSeconds(3600L) // 1 hour
                .maxAllowedEvents(3)  // Max 3 messages allowed
                .targetChannel(ChannelType.SMS)
                .reason("Exceeded 3 SMS per hour")
                .build();
    }

    @Test
    @DisplayName("Should pass first 3 SMS messages and DROP the 4th within the 1-hour window")
    void testRateLimitEnforcement() {
        String userId = "user100";
        UserProfile profile = UserProfile.builder().userId(userId).build();
        Instant now = Instant.now();

        // 1st SMS -> PASS
        Notification notif1 = createSms(userId, "Message 1");
        CepEvaluationResult res1 = evaluator.evaluate(notif1, profile, rateLimitRule, now);
        assertThat(res1.getAction()).isEqualTo(CepAction.PASS);

        // 2nd SMS -> PASS
        Notification notif2 = createSms(userId, "Message 2");
        CepEvaluationResult res2 = evaluator.evaluate(notif2, profile, rateLimitRule, now.plusSeconds(60));
        assertThat(res2.getAction()).isEqualTo(CepAction.PASS);

        // 3rd SMS -> PASS
        Notification notif3 = createSms(userId, "Message 3");
        CepEvaluationResult res3 = evaluator.evaluate(notif3, profile, rateLimitRule, now.plusSeconds(120));
        assertThat(res3.getAction()).isEqualTo(CepAction.PASS);

        // 4th SMS within the hour -> DROP
        Notification notif4 = createSms(userId, "Message 4");
        CepEvaluationResult res4 = evaluator.evaluate(notif4, profile, rateLimitRule, now.plusSeconds(180));
        assertThat(res4.getAction()).isEqualTo(CepAction.DROP);
        assertThat(res4.getTriggeredRuleId()).isEqualTo("RULE-RATE-LIMIT-SMS-002");
    }

    @Test
    @DisplayName("Should not rate-limit notifications on other channels (e.g. EMAIL)")
    void testRateLimitAppliesOnlyToTargetChannel() {
        String userId = "user200";
        UserProfile profile = UserProfile.builder().userId(userId).build();
        Instant now = Instant.now();

        Notification emailNotif = Notification.builder()
                .userId(userId)
                .channel(ChannelType.EMAIL)
                .message("Email content")
                .build();

        // Send 5 emails; none should be dropped by SMS rule
        for (int i = 0; i < 5; i++) {
            CepEvaluationResult res = evaluator.evaluate(emailNotif, profile, rateLimitRule, now.plusSeconds(i * 10));
            assertThat(res.getAction()).isEqualTo(CepAction.PASS);
        }
    }

    @Test
    @DisplayName("Should reset counter once the sliding window duration has passed")
    void testSlidingWindowExpiry() {
        String userId = "user300";
        UserProfile profile = UserProfile.builder().userId(userId).build();
        Instant t0 = Instant.parse("2026-08-19T10:00:00Z");

        // Send 3 SMS messages at 10:00
        for (int i = 1; i <= 3; i++) {
            evaluator.evaluate(createSms(userId, "Msg " + i), profile, rateLimitRule, t0.plusSeconds(i));
        }

        // At 10:30 (within window) -> 4th message should DROP
        Instant t1 = t0.plusSeconds(1800);
        CepEvaluationResult resDrop = evaluator.evaluate(createSms(userId, "Msg 4"), profile, rateLimitRule, t1);
        assertThat(resDrop.getAction()).isEqualTo(CepAction.DROP);

        // At 11:05 (window expired after 3600s) -> Should PASS again
        Instant t2 = t0.plusSeconds(3900);
        CepEvaluationResult resPass = evaluator.evaluate(createSms(userId, "Msg 5"), profile, rateLimitRule, t2);
        assertThat(resPass.getAction()).isEqualTo(CepAction.PASS);
    }

    private Notification createSms(String userId, String message) {
        return Notification.builder()
                .userId(userId)
                .channel(ChannelType.SMS)
                .message(message)
                .build();
    }
}
