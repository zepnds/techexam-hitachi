package com.notification.platform.cep.evaluator;

import com.notification.platform.cep.model.CepEvaluationResult;
import com.notification.platform.cep.model.CepRuleDefinition;
import com.notification.platform.cep.model.CepRuleType;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class QuietHoursRuleEvaluator implements CepRuleEvaluator {

    private static final Logger log = LoggerFactory.getLogger(QuietHoursRuleEvaluator.class);

    @Override
    public CepRuleType supportsType() {
        return CepRuleType.QUIET_HOURS;
    }

    @Override
    public CepEvaluationResult evaluate(Notification notification, UserProfile profile, CepRuleDefinition rule, Instant now) {
        String targetCountry = rule.getTargetCountry();
        if (targetCountry == null && rule.getConditions() != null) {
            targetCountry = (String) rule.getConditions().get("country");
        }
        String effectiveCountry = notification.getCountry();
        if (effectiveCountry == null && profile != null) {
            effectiveCountry = profile.getCountry();
        }

        if (targetCountry != null && effectiveCountry != null) {
            if (!targetCountry.equalsIgnoreCase(effectiveCountry.trim())) {
                return CepEvaluationResult.pass(); 
            }
        } else if (targetCountry != null && effectiveCountry == null) {
            return CepEvaluationResult.pass();
        }

        ZoneId zoneId = ZoneId.of("Asia/Manila"); 
        Map<String, Object> conditions = rule.getConditions();
        if (conditions != null && conditions.containsKey("timezone")) {
            try {
                zoneId = ZoneId.of((String) conditions.get("timezone"));
            } catch (Exception ignored) {
            }
        } else if (profile != null && profile.getTimezone() != null) {
            zoneId = profile.getTimezone();
        }

        int startHour = 22; 
        int endHour = 6;  
        int resumeHour = 6;
        int resumeMinute = 0;

        if (conditions != null) {
            if (conditions.containsKey("quietStartHour")) {
                startHour = ((Number) conditions.get("quietStartHour")).intValue();
            }
            if (conditions.containsKey("quietEndHour")) {
                endHour = ((Number) conditions.get("quietEndHour")).intValue();
            }
            if (conditions.containsKey("resumeHour")) {
                resumeHour = ((Number) conditions.get("resumeHour")).intValue();
            }
            if (conditions.containsKey("resumeMinute")) {
                resumeMinute = ((Number) conditions.get("resumeMinute")).intValue();
            }
        }

        ZonedDateTime localZonedNow = now.atZone(zoneId);
        int currentHour = localZonedNow.getHour();

      
        boolean inQuietHours;
        if (startHour > endHour) {
         
            inQuietHours = (currentHour >= startHour || currentHour < endHour);
        } else {
            
            inQuietHours = (currentHour >= startHour && currentHour < endHour);
        }

        if (inQuietHours) {
          
            LocalDate resumeDate = localZonedNow.toLocalDate();
          
            if (currentHour >= startHour) {
                resumeDate = resumeDate.plusDays(1);
            }
           
            ZonedDateTime resumeZonedDateTime = ZonedDateTime.of(
                    resumeDate,
                    LocalTime.of(resumeHour, resumeMinute),
                    zoneId
            );

            Instant delayUntil = resumeZonedDateTime.toInstant();
            String reason = rule.getReason() != null ? rule.getReason() :
                    String.format("Delayed until %s (%s) due to quiet hours policy in %s",
                            resumeZonedDateTime.toLocalTime(), zoneId.getId(), effectiveCountry);

            log.info("CEP [QUIET_HOURS] Rule '{}' triggered for user {}. Notification delayed until {}",
                    rule.getId(), notification.getUserId(), delayUntil);

            return CepEvaluationResult.delay(rule.getId(), rule.getName(), reason, delayUntil);
        }

        return CepEvaluationResult.pass();
    }
}
