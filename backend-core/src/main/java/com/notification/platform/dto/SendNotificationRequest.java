package com.notification.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.notification.platform.domain.model.ChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to trigger an asynchronous notification")
public class SendNotificationRequest {

    @NotBlank(message = "user_id is required")
    @JsonProperty("user_id")
    @Schema(description = "Unique user identifier", example = "12345", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @JsonProperty("country")
    @Schema(description = "Country code or name for localization and quiet-hours evaluation", example = "PHILIPPINES")
    private String country;

    @NotNull(message = "channel is required (EMAIL, SMS, PUSH, SLACK)")
    @JsonProperty("channel")
    @Schema(description = "Delivery channel type", example = "EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
    private ChannelType channel;

    @NotBlank(message = "message content is required")
    @JsonProperty("message")
    @Schema(description = "Notification message text payload", example = "Your OTP is 9876", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @JsonProperty("schedule_at")
    @Schema(description = "Optional future UTC timestamp for delayed delivery", example = "2026-12-31T18:00:00Z")
    private Instant scheduleAt;

    @JsonProperty("recipient_target")
    @Schema(description = "Optional target phone/email/webhook override", example = "maria.santos@example.ph")
    private String recipientTarget;
}
