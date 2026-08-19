package com.notification.platform.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_user_channel", columnList = "user_id, channel"),
        @Index(name = "idx_notif_status", columnList = "status"),
        @Index(name = "idx_notif_schedule_at", columnList = "schedule_at"),
        @Index(name = "idx_notif_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "auditLogs")
@EqualsAndHashCode(of = "id")
public class Notification implements Persistable<String> {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "country", length = 64)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private ChannelType channel;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "schedule_at")
    private Instant scheduleAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private NotificationStatus status;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private int maxRetries = 3;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "drop_reason", columnDefinition = "TEXT")
    private String dropReason;

    @Column(name = "applied_cep_rule", length = 128)
    private String appliedCepRule;

    @Column(name = "recipient_target", length = 255)
    private String recipientTarget;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<NotificationAuditLog> auditLogs = new ArrayList<>();

    @Transient
    @Builder.Default
    private boolean isNewRecord = true;

    @Override
    public boolean isNew() {
        return this.isNewRecord;
    }

    @PostLoad
    @PostPersist
    public void markNotNew() {
        this.isNewRecord = false;
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = NotificationStatus.QUEUED;
        }
        if (this.auditLogs == null) {
            this.auditLogs = new ArrayList<>();
        }
    }

    public void addAuditLog(NotificationStatus fromStatus, NotificationStatus toStatus, String description) {
        if (this.auditLogs == null) {
            this.auditLogs = new ArrayList<>();
        }
        NotificationAuditLog logEntry = NotificationAuditLog.builder()
                .notification(this)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .description(description)
                .build();
        this.auditLogs.add(logEntry);
    }
}
