package com.notification.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.notification.common.model.CepAction;
import com.notification.common.model.CepRuleType;
import com.notification.common.model.ChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Complex Event Processing (CEP) rule definition DTO")
public class CepRuleDto {

    @Schema(description = "Rule ID", example = "RULE-RATE-LIMIT-SMS-002")
    private String id;

    @Schema(description = "Rule display name", example = "SMS Channel Hourly Rate Limit")
    private String name;

    @Schema(description = "Rule description")
    private String description;

    @JsonProperty("rule_type")
    @Schema(description = "Rule Type: RATE_LIMIT, DEDUPLICATION, QUIET_HOURS, CONTENT_FILTER")
    private CepRuleType ruleType;

    @Schema(description = "Whether the rule is active")
    private boolean enabled;

    @Schema(description = "Evaluation priority (lower values evaluate first)", example = "10")
    private int priority;

    @Schema(description = "Action when rule is triggered: PASS, DROP, DELAY, REROUTE")
    private CepAction action;

    @JsonProperty("window_seconds")
    @Schema(description = "Sliding window duration in seconds", example = "3600")
    private Long windowSeconds;

    @JsonProperty("max_allowed_events")
    @Schema(description = "Max allowed events in window for rate limiting", example = "3")
    private Integer maxAllowedEvents;

    @JsonProperty("target_channel")
    @Schema(description = "Target channel filter")
    private ChannelType targetChannel;

    @JsonProperty("target_country")
    @Schema(description = "Target country filter")
    private String targetCountry;

    @Schema(description = "Custom rule parameter conditions")
    private Map<String, Object> conditions;

    @Schema(description = "Explanation or audit message attached when triggered")
    private String reason;
}
