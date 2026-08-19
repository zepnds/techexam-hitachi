package com.notification.platform.domain.model;

public enum NotificationStatus {
    QUEUED,
    PROCESSING,
    SENT,
    RETRYING,
    FAILED,
    DELAYED,
    DROPPED,
    DEAD_LETTER
}
