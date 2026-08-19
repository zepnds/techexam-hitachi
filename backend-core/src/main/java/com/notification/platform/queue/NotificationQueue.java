package com.notification.platform.queue;

import com.notification.platform.domain.model.Notification;

import java.util.concurrent.TimeUnit;

public interface NotificationQueue {

   
    boolean enqueue(Notification notification);

    
    Notification poll(long timeout, TimeUnit unit) throws InterruptedException;

   
    int size();

   
    boolean isEmpty();
}
