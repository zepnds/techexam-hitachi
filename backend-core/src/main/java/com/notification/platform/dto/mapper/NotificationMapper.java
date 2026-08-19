package com.notification.platform.dto.mapper;

import com.notification.common.dto.*;
import com.notification.platform.cep.model.CepRuleDefinition;
import com.notification.platform.domain.model.ChannelType;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.NotificationAuditLog;
import com.notification.platform.domain.model.NotificationStatus;
import com.notification.platform.domain.model.UserProfile;
import com.notification.platform.domain.model.UserProfileEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class NotificationMapper {

    public Notification toEntity(SendNotificationRequest request) {
        if (request == null) return null;
        return Notification.builder()
                .id(UUID.randomUUID().toString())
                .userId(request.getUserId())
                .country(request.getCountry())
                .channel(request.getChannel() != null ? ChannelType.valueOf(request.getChannel().name()) : null)
                .message(request.getMessage())
                .scheduleAt(request.getScheduleAt())
                .recipientTarget(request.getRecipientTarget())
                .status(NotificationStatus.QUEUED)
                .build();
    }

    public NotificationResponse toResponse(Notification entity) {
        if (entity == null) return null;
        return NotificationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .country(entity.getCountry())
                .channel(entity.getChannel() != null ? com.notification.common.model.ChannelType.valueOf(entity.getChannel().name()) : null)
                .message(entity.getMessage())
                .status(entity.getStatus() != null ? com.notification.common.model.NotificationStatus.valueOf(entity.getStatus().name()) : null)
                .scheduleAt(entity.getScheduleAt())
                .retryCount(entity.getRetryCount())
                .failureReason(entity.getFailureReason())
                .dropReason(entity.getDropReason())
                .appliedCepRule(entity.getAppliedCepRule())
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .auditLogs(toAuditLogResponseList(entity.getAuditLogs()))
                .build();
    }

    public NotificationStatusResponse toStatusResponse(Notification entity) {
        if (entity == null) return null;
        return NotificationStatusResponse.builder()
                .id(entity.getId())
                .status(entity.getStatus() != null ? com.notification.common.model.NotificationStatus.valueOf(entity.getStatus().name()) : null)
                .retryCount(entity.getRetryCount())
                .failureReason(entity.getFailureReason())
                .dropReason(entity.getDropReason())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public NotificationAuditLogResponse toAuditLogResponse(NotificationAuditLog logEntry) {
        if (logEntry == null) return null;
        return NotificationAuditLogResponse.builder()
                .id(logEntry.getId())
                .fromStatus(logEntry.getFromStatus() != null ? com.notification.common.model.NotificationStatus.valueOf(logEntry.getFromStatus().name()) : null)
                .toStatus(logEntry.getToStatus() != null ? com.notification.common.model.NotificationStatus.valueOf(logEntry.getToStatus().name()) : null)
                .description(logEntry.getDescription())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }

    public List<NotificationAuditLogResponse> toAuditLogResponseList(List<NotificationAuditLog> logs) {
        if (logs == null || logs.isEmpty()) return Collections.emptyList();
        return logs.stream().map(this::toAuditLogResponse).toList();
    }

    public CepRuleDto toCepRuleDto(CepRuleDefinition rule) {
        if (rule == null) return null;
        return CepRuleDto.builder()
                .id(rule.getId())
                .name(rule.getName())
                .description(rule.getDescription())
                .ruleType(rule.getRuleType() != null ? com.notification.common.model.CepRuleType.valueOf(rule.getRuleType().name()) : null)
                .enabled(rule.isEnabled())
                .priority(rule.getPriority())
                .action(rule.getAction() != null ? com.notification.common.model.CepAction.valueOf(rule.getAction().name()) : null)
                .windowSeconds(rule.getWindowSeconds())
                .maxAllowedEvents(rule.getMaxAllowedEvents())
                .targetChannel(rule.getTargetChannel() != null ? com.notification.common.model.ChannelType.valueOf(rule.getTargetChannel().name()) : null)
                .targetCountry(rule.getTargetCountry())
                .conditions(rule.getConditions())
                .reason(rule.getReason())
                .build();
    }

    public List<CepRuleDto> toCepRuleDtoList(List<CepRuleDefinition> rules) {
        if (rules == null || rules.isEmpty()) return Collections.emptyList();
        return rules.stream().map(this::toCepRuleDto).toList();
    }

    public UserProfileDto toUserProfileDto(UserProfile profile) {
        if (profile == null) return null;
        return UserProfileDto.builder()
                .userId(profile.getUserId())
                .name(profile.getName())
                .email(profile.getEmail())
                .phoneNumber(profile.getPhoneNumber())
                .country(profile.getCountry())
                .timezone(profile.getTimezone() != null ? profile.getTimezone().getId() : null)
                .tier(profile.getTier())
                .quietHoursOptIn(profile.isQuietHoursOptIn())
                .build();
    }

    public UserProfileDto toUserProfileDto(UserProfileEntity entity) {
        if (entity == null) return null;
        return UserProfileDto.builder()
                .userId(entity.getUserId())
                .name(entity.getName())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .country(entity.getCountry())
                .timezone(entity.getTimezone())
                .tier(entity.getTier())
                .quietHoursOptIn(entity.isQuietHoursOptIn())
                .build();
    }
}
