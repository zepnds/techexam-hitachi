package com.notification.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response returned after hot-reloading CEP rules")
public class ReloadRulesResponse {

    @Schema(description = "Status of reload operation", example = "SUCCESS")
    private String status;

    @Schema(description = "Informative status message", example = "CEP rules successfully reloaded")
    private String message;

    @JsonProperty("rule_count")
    @Schema(description = "Total active rules loaded", example = "4")
    private int ruleCount;
}
