package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.service.NotificationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<?> listNotifications(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(notificationService.listNotifications(userId, type, isRead, page, size));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<?> markAsRead(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        notificationService.markAsRead(userId, id);
        return ApiResponse.success("已标记为已读", null);
    }

    @PutMapping("/read-all")
    public ApiResponse<?> markAllRead(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success("已全部标记为已读", notificationService.markAllRead(userId));
    }
}
