package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.service.LoveService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/love")
public class LoveController {

    private final LoveService loveService;

    public LoveController(LoveService loveService) {
        this.loveService = loveService;
    }

    @GetMapping("/requests")
    public ApiResponse<?> listLoveRequests(
            @RequestParam(defaultValue = "published") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(loveService.listLoveRequests(sortBy, page, size));
    }

    @PostMapping("/requests")
    public ApiResponse<?> createLoveRequest(@RequestAttribute("userId") Long userId,
                                             @RequestBody Map<String, Object> body) {
        String description = (String) body.get("description");
        int validDays = body.get("validDays") instanceof Number ? ((Number) body.get("validDays")).intValue() : 7;
        String scope = (String) body.getOrDefault("scope", "sameSchool");
        return ApiResponse.success("交友需求发布成功，待审核",
                loveService.createLoveRequest(userId, description, validDays, scope));
    }

    @PostMapping("/requests/{requestId}/heart")
    public ApiResponse<?> sendHeart(@RequestAttribute("userId") Long userId, @PathVariable Long requestId) {
        return ApiResponse.success("心动已发送（对方不可见，双向心动后匹配）",
                loveService.sendHeart(userId, requestId));
    }

    @GetMapping("/matches")
    public ApiResponse<?> listMatches(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(loveService.listMatches(userId, status, page, size));
    }
}
