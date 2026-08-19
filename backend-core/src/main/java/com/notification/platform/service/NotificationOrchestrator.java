package com.notification.platform.service;

import com.notification.common.dto.SendNotificationRequest;
import com.notification.platform.domain.model.Notification;

public interface NotificationOrchestrator {

    Notification processNotificationRequest(SendNotificationRequest request);
}
