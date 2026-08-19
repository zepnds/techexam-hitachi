package com.notification.platform.cep.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.notification.platform.domain.model.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CepRuleDefinition {
    private String id;
    private String name;
    private String description;
    private CepRuleType ruleType;
    @Builder.Default
    private boolean enabled = true;
    @Builder.Default
    private int priority = 100;
    private CepAction action;
    private Long windowSeconds;
    private Integer maxAllowedEvents;
    private ChannelType targetChannel;
    private String targetCountry;
    private Map<String, Object> conditions;
    private String reason;
}
