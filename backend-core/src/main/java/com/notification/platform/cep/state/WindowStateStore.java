package com.notification.platform.cep.state;

import java.time.Duration;
import java.time.Instant;

public interface WindowStateStore {

   
    void recordEvent(String key, Instant timestamp);

   
    int countEventsInWindow(String key, Duration windowDuration, Instant now);

   
    boolean checkAndRecordDuplicate(String key, Duration windowDuration, Instant now);

    
    void clear();

    
    void pruneExpired(Instant now);
}
