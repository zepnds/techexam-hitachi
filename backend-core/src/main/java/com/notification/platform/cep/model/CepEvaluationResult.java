package com.notification.platform.cep.model;

import com.notification.platform.domain.model.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CepEvaluationResult {
    @Builder.Default
    private boolean passed = true;
    @Builder.Default
    private CepAction action = CepAction.PASS;
    private String triggeredRuleId;
    private String ruleName;
    private String reason;
    private Instant delayUntil;
    private ChannelType reroutedChannel;

    public static CepEvaluationResult pass() {
        return CepEvaluationResult.builder()
                .passed(true)
                .action(CepAction.PASS)
                .build();
    }

    public static CepEvaluationResult drop(String ruleId, String ruleName, String reason) {
        return CepEvaluationResult.builder()
                .passed(false)
                .action(CepAction.DROP)
                .triggeredRuleId(ruleId)
                .ruleName(ruleName)
                .reason(reason)
                .build();
    }

    public static CepEvaluationResult delay(String ruleId, String ruleName, String reason, Instant delayUntil) {
        return CepEvaluationResult.builder()
                .passed(false)
                .action(CepAction.DELAY)
                .triggeredRuleId(ruleId)
                .ruleName(ruleName)
                .reason(reason)
                .delayUntil(delayUntil)
                .build();
    }

    public static CepEvaluationResult reroute(String ruleId, String ruleName, String reason, ChannelType newChannel) {
        return CepEvaluationResult.builder()
                .passed(true)
                .action(CepAction.REROUTE)
                .triggeredRuleId(ruleId)
                .ruleName(ruleName)
                .reason(reason)
                .reroutedChannel(newChannel)
                .build();
    }
}
