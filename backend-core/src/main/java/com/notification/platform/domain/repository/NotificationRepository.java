package com.notification.platform.domain.repository;

import com.notification.platform.domain.model.ChannelType;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.scheduleAt <= :now")
    List<Notification> findDueScheduledNotifications(
            @Param("status") NotificationStatus status,
            @Param("now") Instant now
    );

    long countByUserIdAndChannelAndStatusAndCreatedAtAfter(
            String userId,
            ChannelType channel,
            NotificationStatus status,
            Instant after
    );
}
