package com.notification.platform.scheduler;

import com.notification.platform.domain.model.Notification;

import java.time.Instant;

public interface NotificationSchedulerService {

  
    void scheduleDelayedDelivery(Notification notification, Instant targetDeliveryTime);

   
    void scheduleRetry(Notification notification, long delayMs);

    void pollDueScheduledNotifications();
}
