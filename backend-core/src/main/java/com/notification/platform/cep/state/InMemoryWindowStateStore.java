package com.notification.platform.cep.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class InMemoryWindowStateStore implements WindowStateStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryWindowStateStore.class);

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Instant>> rateLimitWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> deduplicationIndex = new ConcurrentHashMap<>();

    @Override
    public void recordEvent(String key, Instant timestamp) {
        rateLimitWindows.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>()).addLast(timestamp);
    }

    @Override
    public int countEventsInWindow(String key, Duration windowDuration, Instant now) {
        ConcurrentLinkedDeque<Instant> timestamps = rateLimitWindows.get(key);
        if (timestamps == null || timestamps.isEmpty()) {
            return 0;
        }

        Instant cutoff = now.minus(windowDuration);
      
        while (!timestamps.isEmpty()) {
            Instant oldest = timestamps.peekFirst();
            if (oldest != null && oldest.isBefore(cutoff)) {
                timestamps.pollFirst();
            } else {
                break;
            }
        }
        return timestamps.size();
    }

    @Override
    public boolean checkAndRecordDuplicate(String key, Duration windowDuration, Instant now) {
        Instant cutoff = now.minus(windowDuration);
        final boolean[] isDuplicate = {false};

        deduplicationIndex.compute(key, (k, lastSeen) -> {
            if (lastSeen != null && lastSeen.isAfter(cutoff)) {
                isDuplicate[0] = true;
                return lastSeen;
            } else {
                isDuplicate[0] = false;
                return now; 
            }
        });

        return isDuplicate[0];
    }

    @Override
    public void clear() {
        rateLimitWindows.clear();
        deduplicationIndex.clear();
    }

    @Scheduled(fixedRate = 60000) // Every 1 minute
    public void scheduledCleanup() {
        pruneExpired(Instant.now());
    }

    @Override
    public void pruneExpired(Instant now) {
        // Prune rate limit windows older than 24 hours
        Instant rateLimitCutoff = now.minus(Duration.ofHours(24));
        rateLimitWindows.entrySet().removeIf(entry -> {
            ConcurrentLinkedDeque<Instant> deque = entry.getValue();
            while (!deque.isEmpty()) {
                Instant first = deque.peekFirst();
                if (first != null && first.isBefore(rateLimitCutoff)) {
                    deque.pollFirst();
                } else {
                    break;
                }
            }
            return deque.isEmpty();
        });

        // Prune deduplication index older than 1 hour
        Instant dedupCutoff = now.minus(Duration.ofHours(1));
        deduplicationIndex.entrySet().removeIf(entry -> entry.getValue().isBefore(dedupCutoff));
        log.debug("State store cleanup executed. Active rate keys: {}, dedup keys: {}", 
                rateLimitWindows.size(), deduplicationIndex.size());
    }
}
