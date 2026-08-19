package com.notification.platform.resilience;

import com.notification.platform.domain.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeadLetterQueueService {

  
    void routeToDeadLetter(Notification notification, String terminalReason);

    
    Page<Notification> getDeadLetterMessages(Pageable pageable);
}
