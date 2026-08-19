package com.notification.platform.service.impl;

import com.notification.common.dto.NotificationResponse;
import com.notification.common.dto.NotificationStatusResponse;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.repository.NotificationRepository;
import com.notification.platform.dto.mapper.NotificationMapper;
import com.notification.platform.service.NotificationQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public Notification getNotificationEntity(String id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Notification not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotification(String id) {
        Notification notification = getNotificationEntity(id);
        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationStatusResponse getNotificationStatus(String id) {
        Notification notification = getNotificationEntity(id);
        return notificationMapper.toStatusResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserId(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }
}
