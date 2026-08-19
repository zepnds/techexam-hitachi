package com.notification.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.notification.common.model.NotificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification Status tracking response")
public class NotificationStatusResponse {

    @Schema(description = "Notification ID", example = "9f87c80b-1175-47fe-bb0c-358fe2ecbf42")
    private String id;

    @Schema(description = "Current lifecycle status: QUEUED, PROCESSING, SENT, RETRYING, FAILED, DELAYED, DROPPED, DEAD_LETTER", example = "SENT")
    private NotificationStatus status;

    @JsonProperty("retry_count")
    @Schema(description = "Current retry count", example = "0")
    private int retryCount;

    @JsonProperty("failure_reason")
    @Schema(description = "Failure details if status is FAILED, RETRYING, or DEAD_LETTER")
    private String failureReason;

    @JsonProperty("drop_reason")
    @Schema(description = "Drop reason if status is DROPPED")
    private String dropReason;

    @JsonProperty("updated_at")
    @Schema(description = "Timestamp of last status update")
    private Instant updatedAt;
}
