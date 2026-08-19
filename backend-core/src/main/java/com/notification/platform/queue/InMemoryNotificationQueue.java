package com.notification.platform.queue;

import com.notification.platform.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class InMemoryNotificationQueue implements NotificationQueue {

    private static final Logger log = LoggerFactory.getLogger(InMemoryNotificationQueue.class);
    private final LinkedBlockingQueue<Notification> queue;

    public InMemoryNotificationQueue(@Value("${notification.queue.capacity:10000}") int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
        log.info("Initialized InMemoryNotificationQueue with bounded capacity: {}", capacity);
    }

    @Override
    public boolean enqueue(Notification notification) {
        boolean offered = queue.offer(notification);
        if (!offered) {
            log.error("Notification Queue is FULL! Dropping/Rejecting notification: {}", notification.getId());
        }
        return offered;
    }

    @Override
    public Notification poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
