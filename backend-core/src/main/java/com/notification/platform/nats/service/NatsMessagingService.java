package com.notification.platform.nats.service;

import com.notification.platform.domain.model.Notification;

public interface NatsMessagingService {

    boolean isConnected();

    void publishNotificationEvent(Notification notification);

    void publishDlqEvent(Notification notification, String terminalReason);
}
