package com.notification.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.notification.platform.domain.model.NotificationStatus;
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
@Schema(description = "Notification status audit log entry")
public class NotificationAuditLogResponse {

    @Schema(description = "Audit Log ID", example = "1")
    private Long id;

    @JsonProperty("from_status")
    @Schema(description = "Previous lifecycle status")
    private NotificationStatus fromStatus;

    @JsonProperty("to_status")
    @Schema(description = "New lifecycle status")
    private NotificationStatus toStatus;

    @Schema(description = "Audit transition description")
    private String description;

    @JsonProperty("created_at")
    @Schema(description = "Timestamp when the status transition was recorded")
    private Instant createdAt;
}
