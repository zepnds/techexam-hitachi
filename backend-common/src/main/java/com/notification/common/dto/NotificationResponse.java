package com.notification.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.notification.common.model.ChannelType;
import com.notification.common.model.NotificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Full Notification details response")
public class NotificationResponse {

    @Schema(description = "Notification unique ID", example = "9f87c80b-1175-47fe-bb0c-358fe2ecbf42")
    private String id;

    @JsonProperty("user_id")
    @Schema(description = "User identifier", example = "12345")
    private String userId;

    @Schema(description = "Country", example = "PHILIPPINES")
    private String country;

    @Schema(description = "Target channel", example = "EMAIL")
    private ChannelType channel;

    @Schema(description = "Notification message content", example = "Your OTP is 9876")
    private String message;

    @Schema(description = "Current lifecycle status", example = "SENT")
    private NotificationStatus status;

    @JsonProperty("schedule_at")
    @Schema(description = "Scheduled execution timestamp if delayed")
    private Instant scheduleAt;

    @JsonProperty("retry_count")
    @Schema(description = "Number of retry attempts executed", example = "0")
    private int retryCount;

    @JsonProperty("failure_reason")
    @Schema(description = "Detailed failure reason if unsuccessful")
    private String failureReason;

    @JsonProperty("drop_reason")
    @Schema(description = "Detailed drop explanation if suppressed by CEP")
    private String dropReason;

    @JsonProperty("applied_cep_rule")
    @Schema(description = "Identifier of CEP rule applied (if any)", example = "RULE-RATE-LIMIT-SMS-002")
    private String appliedCepRule;

    @JsonProperty("created_at")
    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @JsonProperty("sent_at")
    @Schema(description = "Sent timestamp")
    private Instant sentAt;

    @JsonProperty("audit_logs")
    @Schema(description = "History of status transitions recorded in JPA ORM")
    private List<NotificationAuditLogResponse> auditLogs;
}
