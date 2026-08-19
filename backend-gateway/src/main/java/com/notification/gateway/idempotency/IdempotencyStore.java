package com.notification.gateway.idempotency;

import com.notification.common.dto.NotificationResponse;

import java.util.Optional;

public interface IdempotencyStore {

    Optional<NotificationResponse> get(String key);

    void save(String key, NotificationResponse response);
}
