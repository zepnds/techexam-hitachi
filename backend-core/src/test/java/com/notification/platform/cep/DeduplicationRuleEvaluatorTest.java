package com.notification.platform.cep;

import com.notification.platform.cep.evaluator.DeduplicationRuleEvaluator;
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

class DeduplicationRuleEvaluatorTest {

    private InMemoryWindowStateStore stateStore;
    private DeduplicationRuleEvaluator evaluator;
    private CepRuleDefinition dedupRule;

    @BeforeEach
    void setUp() {
        stateStore = new InMemoryWindowStateStore();
        evaluator = new DeduplicationRuleEvaluator(stateStore);

        dedupRule = CepRuleDefinition.builder()
                .id("RULE-DEDUP-001")
                .name("Message Deduplication Rule")
                .ruleType(CepRuleType.DEDUPLICATION)
                .enabled(true)
                .action(CepAction.DROP)
                .windowSeconds(300L) // 5 minutes
                .reason("Duplicate message suppressed")
                .build();
    }

    @Test
    @DisplayName("Should suppress identical message sent to same user within 5 minutes")
    void testDuplicateMessageSuppressed() {
        String userId = "user_dedup_01";
        UserProfile profile = UserProfile.builder().userId(userId).build();
        Instant now = Instant.now();

        Notification msg1 = Notification.builder()
                .userId(userId)
                .channel(ChannelType.EMAIL)
                .message("Your verification code is 445566")
                .build();

        Notification msg2Duplicate = Notification.builder()
                .userId(userId)
                .channel(ChannelType.SMS)
                .message("Your verification code is 445566")
                .build();

        // 1st occurrence -> PASS
        CepEvaluationResult res1 = evaluator.evaluate(msg1, profile, dedupRule, now);
        assertThat(res1.getAction()).isEqualTo(CepAction.PASS);

        // 2nd occurrence 2 minutes later -> DROP
        CepEvaluationResult res2 = evaluator.evaluate(msg2Duplicate, profile, dedupRule, now.plusSeconds(120));
        assertThat(res2.getAction()).isEqualTo(CepAction.DROP);
        assertThat(res2.getTriggeredRuleId()).isEqualTo("RULE-DEDUP-001");
    }

    @Test
    @DisplayName("Should allow same message to different users")
    void testDifferentUsersNotSuppressed() {
        Instant now = Instant.now();
        String message = "System Maintenance at midnight";

        Notification userANotif = Notification.builder().userId("userA").message(message).channel(ChannelType.EMAIL).build();
        Notification userBNotif = Notification.builder().userId("userB").message(message).channel(ChannelType.EMAIL).build();

        CepEvaluationResult resA = evaluator.evaluate(userANotif, null, dedupRule, now);
        CepEvaluationResult resB = evaluator.evaluate(userBNotif, null, dedupRule, now.plusSeconds(10));

        assertThat(resA.getAction()).isEqualTo(CepAction.PASS);
        assertThat(resB.getAction()).isEqualTo(CepAction.PASS);
    }

    @Test
    @DisplayName("Should allow identical message after the 5-minute window expires")
    void testDuplicateAllowedAfterWindowExpiry() {
        String userId = "user_window_expiry";
        Instant t0 = Instant.parse("2026-08-19T12:00:00Z");

        Notification msg = Notification.builder().userId(userId).message("OTP 1234").channel(ChannelType.SMS).build();

        // 1st message at 12:00 -> PASS
        CepEvaluationResult res1 = evaluator.evaluate(msg, null, dedupRule, t0);
        assertThat(res1.getAction()).isEqualTo(CepAction.PASS);

        // At 12:06 (360s later, window is 300s) -> PASS
        Instant t1 = t0.plusSeconds(360);
        CepEvaluationResult res2 = evaluator.evaluate(msg, null, dedupRule, t1);
        assertThat(res2.getAction()).isEqualTo(CepAction.PASS);
    }
}
