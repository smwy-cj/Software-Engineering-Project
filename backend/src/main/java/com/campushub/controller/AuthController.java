package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.dto.request.*;
import com.campushub.entity.UserCert;
import com.campushub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.success("注册成功", userService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.success("登录成功", userService.login(req));
    }

    @PostMapping("/sms-code")
    public ApiResponse<?> sendSmsCode(@Valid @RequestBody SmsCodeRequest req) {
        return ApiResponse.success("验证码已发送", userService.sendSmsCode(req));
    }

    @GetMapping("/captcha")
    public ApiResponse<?> getCaptcha() {
        return ApiResponse.success(userService.getCaptcha());
    }

    @PostMapping("/certify")
    public ApiResponse<?> certify(@RequestAttribute("userId") Long userId, @RequestBody UserCert certData) {
        return ApiResponse.success("实名认证成功", userService.certify(userId, certData));
    }

    @GetMapping("/cert-status")
    public ApiResponse<?> getCertStatus(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(userService.getCertStatus(userId));
    }

    @PutMapping("/profile")
    public ApiResponse<?> updateProfile(@RequestAttribute("userId") Long userId,
                                         @Valid @RequestBody UserProfileRequest req) {
        return ApiResponse.success("个人资料更新成功", userService.updateProfile(userId, req));
    }
}
