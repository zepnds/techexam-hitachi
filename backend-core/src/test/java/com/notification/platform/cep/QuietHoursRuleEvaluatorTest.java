package com.notification.platform.cep;

import com.notification.platform.cep.evaluator.QuietHoursRuleEvaluator;
import com.notification.platform.cep.model.CepAction;
import com.notification.platform.cep.model.CepEvaluationResult;
import com.notification.platform.cep.model.CepRuleDefinition;
import com.notification.platform.cep.model.CepRuleType;
import com.notification.platform.domain.model.ChannelType;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuietHoursRuleEvaluatorTest {

    private QuietHoursRuleEvaluator evaluator;
    private CepRuleDefinition quietHoursRule;

    @BeforeEach
    void setUp() {
        evaluator = new QuietHoursRuleEvaluator();

        quietHoursRule = CepRuleDefinition.builder()
                .id("RULE-QUIET-HOURS-PH-003")
                .name("Philippines Quiet Hours Routing")
                .ruleType(CepRuleType.QUIET_HOURS)
                .enabled(true)
                .action(CepAction.DELAY)
                .targetCountry("PHILIPPINES")
                .conditions(Map.of(
                        "country", "PHILIPPINES",
                        "quietStartHour", 22,
                        "quietEndHour", 6,
                        "resumeHour", 6,
                        "resumeMinute", 0,
                        "timezone", "Asia/Manila"
                ))
                .reason("Delayed until morning 06:00 due to quiet hours")
                .build();
    }

    @Test
    @DisplayName("Should DELAY notification when sent at 11:30 PM (23:30) Manila time until next morning 06:00 AM")
    void testQuietHoursDelayAtNight() {
        String userId = "user_ph_night";
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .country("PHILIPPINES")
                .timezone(ZoneId.of("Asia/Manila"))
                .build();

        Notification notif = Notification.builder()
                .userId(userId)
                .country("PHILIPPINES")
                .channel(ChannelType.SMS)
                .message("Flash Sale Notice")
                .build();

        // 11:30 PM Manila time (23:30 Manila = 15:30 UTC)
        Instant lateNightUtc = Instant.parse("2026-08-19T15:30:00Z");

        CepEvaluationResult result = evaluator.evaluate(notif, profile, quietHoursRule, lateNightUtc);

        assertThat(result.getAction()).isEqualTo(CepAction.DELAY);
        assertThat(result.getTriggeredRuleId()).isEqualTo("RULE-QUIET-HOURS-PH-003");
        assertThat(result.getDelayUntil()).isNotNull();

        // Verify the resume time is 06:00 AM Manila time (22:00 UTC of 2026-08-19)
        ZonedDateTime delayZoned = result.getDelayUntil().atZone(ZoneId.of("Asia/Manila"));
        assertThat(delayZoned.getHour()).isEqualTo(6);
        assertThat(delayZoned.getMinute()).isEqualTo(0);
        assertThat(delayZoned.getDayOfMonth()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should PASS notification when sent at 2:00 PM Manila time")
    void testQuietHoursPassDuringDaytime() {
        String userId = "user_ph_day";
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .country("PHILIPPINES")
                .timezone(ZoneId.of("Asia/Manila"))
                .build();

        Notification notif = Notification.builder()
                .userId(userId)
                .country("PHILIPPINES")
                .channel(ChannelType.EMAIL)
                .message("Daytime notification")
                .build();

        // 2:00 PM Manila time (14:00 Manila = 06:00 UTC)
        Instant daytimeUtc = Instant.parse("2026-08-19T06:00:00Z");

        CepEvaluationResult result = evaluator.evaluate(notif, profile, quietHoursRule, daytimeUtc);

        assertThat(result.getAction()).isEqualTo(CepAction.PASS);
    }

    @Test
    @DisplayName("Should PASS notifications for users outside Philippines (e.g. USA)")
    void testDifferentCountryNotAffected() {
        UserProfile profileUs = UserProfile.builder()
                .userId("user_us")
                .country("UNITED_STATES")
                .timezone(ZoneId.of("America/New_York"))
                .build();

        Notification notif = Notification.builder()
                .userId("user_us")
                .country("UNITED_STATES")
                .channel(ChannelType.SMS)
                .message("US Notice")
                .build();

        Instant nightUtc = Instant.parse("2026-08-19T15:30:00Z");

        CepEvaluationResult result = evaluator.evaluate(notif, profileUs, quietHoursRule, nightUtc);
        assertThat(result.getAction()).isEqualTo(CepAction.PASS);
    }
}
