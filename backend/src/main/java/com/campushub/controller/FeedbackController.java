package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.dto.request.FeedbackRequest;
import com.campushub.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

    private final AdminService adminService;

    public FeedbackController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ApiResponse<?> submitFeedback(@RequestAttribute("userId") Long userId,
                                          @Valid @RequestBody FeedbackRequest req) {
        return ApiResponse.success("反馈提交成功",
                adminService.createFeedback(userId, req.getType(), req.getContent(), req.getContact()));
    }

    @GetMapping("/me")
    public ApiResponse<?> listMyFeedback(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.listMyFeedback(userId, status, page, size));
    }
}
