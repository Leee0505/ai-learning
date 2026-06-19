package com.ticket.service;

import com.ticket.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> listNotifications(Long userId, int page, int size);
    long getUnreadCount(Long userId);
    void markRead(Long notificationId, Long userId);
    void markAllRead(Long userId);
}
