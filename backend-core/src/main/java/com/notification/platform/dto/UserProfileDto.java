package com.notification.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile DTO for notification routing and enrichment")
public class UserProfileDto {

    @NotBlank(message = "user_id is required")
    @JsonProperty("user_id")
    @Schema(description = "User unique ID", example = "12345")
    private String userId;

    @Schema(description = "Full name", example = "Maria Santos")
    private String name;

    @Schema(description = "Email address", example = "maria.santos@example.ph")
    private String email;

    @JsonProperty("phone_number")
    @Schema(description = "E.164 phone number", example = "+639171234567")
    private String phoneNumber;

    @Schema(description = "Country", example = "PHILIPPINES")
    private String country;

    @Schema(description = "Timezone identifier", example = "Asia/Manila")
    private String timezone;

    @Schema(description = "User tier (VIP, STANDARD, BASIC)", example = "VIP")
    private String tier;

    @JsonProperty("quiet_hours_opt_in")
    @Schema(description = "Whether the user opted in for quiet hours routing", example = "true")
    private boolean quietHoursOptIn;
}
