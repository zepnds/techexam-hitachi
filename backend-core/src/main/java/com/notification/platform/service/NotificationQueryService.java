package com.notification.platform.service;

import com.notification.common.dto.NotificationResponse;
import com.notification.common.dto.NotificationStatusResponse;
import com.notification.platform.domain.model.Notification;

import java.util.List;

public interface NotificationQueryService {

    Notification getNotificationEntity(String id);

    NotificationResponse getNotification(String id);

    NotificationStatusResponse getNotificationStatus(String id);

    List<NotificationResponse> getNotificationsByUserId(String userId);
}
