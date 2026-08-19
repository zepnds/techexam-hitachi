package com.notification.gateway.idempotency;

import com.notification.common.dto.NotificationResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private static final long DEFAULT_TTL_SECONDS = 86400; // 24 hours TTL

    private final Map<String, CacheEntry> store = new ConcurrentHashMap<>();

    private record CacheEntry(NotificationResponse response, Instant expiryTime) {}

    @Override
    public Optional<NotificationResponse> get(String key) {
        if (key == null || key.isBlank()) return Optional.empty();

        CacheEntry entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }

        if (Instant.now().isAfter(entry.expiryTime())) {
            store.remove(key);
            return Optional.empty();
        }

        return Optional.of(entry.response());
    }

    @Override
    public void save(String key, NotificationResponse response) {
        if (key == null || key.isBlank() || response == null) return;
        Instant expiry = Instant.now().plusSeconds(DEFAULT_TTL_SECONDS);
        store.put(key, new CacheEntry(response, expiry));
    }
}
