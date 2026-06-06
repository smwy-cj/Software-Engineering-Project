package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.service.ProfileActivityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileActivityController {
    private final ProfileActivityService profileActivityService;

    public ProfileActivityController(ProfileActivityService profileActivityService) {
        this.profileActivityService = profileActivityService;
    }

    @GetMapping("/stats")
    public ApiResponse<?> getStats(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(profileActivityService.getStats(userId));
    }

    @GetMapping("/published")
    public ApiResponse<?> listPublished(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(profileActivityService.listPublished(userId));
    }

    @GetMapping("/applications")
    public ApiResponse<?> listApplications(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(profileActivityService.listApplications(userId));
    }
}
