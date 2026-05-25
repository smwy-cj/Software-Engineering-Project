package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.dto.request.AdminReviewRequest;
import com.campushub.dto.request.BanUserRequest;
import com.campushub.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/reviews/pending")
    public ApiResponse<?> listPendingReviews(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String contentType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.listPendingReviews(userId, contentType, page, size));
    }

    @GetMapping("/reviews/{reviewId}")
    public ApiResponse<?> getReviewDetail(@RequestAttribute("userId") Long userId, @PathVariable Long reviewId) {
        return ApiResponse.success(adminService.getReviewDetail(userId, reviewId));
    }

    @PutMapping("/reviews/{reviewId}")
    public ApiResponse<?> submitReviewResult(@RequestAttribute("userId") Long userId,
                                              @PathVariable Long reviewId,
                                              @Valid @RequestBody AdminReviewRequest req) {
        return ApiResponse.success("审核完成",
                adminService.submitReviewResult(userId, reviewId, req.getResult(), req.getComment()));
    }

    @PostMapping("/users/{targetUserId}/ban")
    public ApiResponse<?> banUser(@RequestAttribute("userId") Long userId,
                                   @PathVariable Long targetUserId,
                                   @Valid @RequestBody BanUserRequest req) {
        return ApiResponse.success("用户处罚成功",
                adminService.banUser(userId, targetUserId, req.getAction(), req.getReason(), req.getDuration()));
    }

    @GetMapping("/feedback")
    public ApiResponse<?> listAllFeedback(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.listAllFeedback(userId, status, type, page, size));
    }

    @PutMapping("/feedback/{feedbackNumber}")
    public ApiResponse<?> processFeedback(@RequestAttribute("userId") Long userId,
                                           @PathVariable String feedbackNumber,
                                           @RequestBody Map<String, String> body) {
        adminService.processFeedback(userId, feedbackNumber, body.get("status"), body.get("processComment"));
        return ApiResponse.success("反馈处理状态已更新", null);
    }
}
