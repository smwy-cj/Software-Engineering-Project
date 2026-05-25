package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.common.PageResult;
import com.campushub.entity.Notification;
import com.campushub.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void createNotification(Long userId, String type, String title, String content,
                                   String relatedType, Long relatedId) {
        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setType(type);
        notif.setTitle(title);
        notif.setContent(content);
        notif.setRelatedType(relatedType);
        notif.setRelatedId(relatedId);
        notificationRepository.save(notif);
    }

    public Map<String, Object> listNotifications(Long userId, String type, Boolean isRead, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (Notification n : result.getContent()) {
            if (type != null && !type.equals(n.getType())) continue;
            if (isRead != null && isRead != n.getIsRead()) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", n.getId());
            item.put("type", n.getType());
            item.put("title", n.getTitle());
            item.put("content", n.getContent());
            item.put("isRead", n.getIsRead());
            item.put("relatedType", n.getRelatedType());
            item.put("relatedId", n.getRelatedId());
            item.put("createdAt", n.getCreatedAt());
            content.add(item);
        }

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", content);
        data.put("page", page);
        data.put("size", size);
        data.put("totalElements", result.getTotalElements());
        data.put("unreadCount", unreadCount);
        return data;
    }

    @Transactional
    public void markAsRead(Long userId, Long notifId) {
        Notification notif = notificationRepository.findById(notifId)
                .orElseThrow(() -> new BusinessException(40401, "通知不存在"));
        if (!notif.getUserId().equals(userId)) {
            throw new BusinessException(40301, "无权操作此通知");
        }
        notif.setIsRead(true);
        notificationRepository.save(notif);
    }

    @Transactional
    public Map<String, Object> markAllRead(Long userId) {
        int count = notificationRepository.markAllReadByUserId(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("updatedCount", count);
        return data;
    }
}
