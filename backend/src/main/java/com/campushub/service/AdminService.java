package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.common.PageResult;
import com.campushub.entity.*;
import com.campushub.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final FeedbackRepository feedbackRepository;
    private final NotificationService notificationService;
    private final ContentReviewService contentReviewService;

    public AdminService(AdminRepository adminRepository, UserRepository userRepository,
                        ReviewRecordRepository reviewRecordRepository,
                        FeedbackRepository feedbackRepository,
                        NotificationService notificationService,
                        ContentReviewService contentReviewService) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.feedbackRepository = feedbackRepository;
        this.notificationService = notificationService;
        this.contentReviewService = contentReviewService;
    }

    private void checkAdmin(Long userId) {
        if (!adminRepository.existsByUserId(userId)) {
            throw new BusinessException(40301, "权限不足，需要管理员权限");
        }
    }

    public PageResult<Map<String, Object>> listPendingReviews(Long adminUserId, String contentType, int page, int size) {
        checkAdmin(adminUserId);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "reviewTime"));
        Page<ReviewRecord> result;
        if (contentType != null) {
            result = reviewRecordRepository.findByContentType(contentType, pageable);
        } else {
            result = reviewRecordRepository.findAll(pageable);
        }

        List<Map<String, Object>> content = new ArrayList<>();
        for (ReviewRecord r : result.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("reviewId", r.getId());
            item.put("contentType", r.getContentType());
            item.put("contentId", r.getContentId());
            item.put("userId", r.getUserId());
            item.put("submitTime", r.getSubmitTime());
            content.add(item);
        }

        return new PageResult<>(content, page, size, result.getTotalElements());
    }

    public Object getReviewDetail(Long adminUserId, Long reviewId) {
        checkAdmin(adminUserId);
        ReviewRecord reviewRecord = reviewRecordRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(40401, "审核记录不存在"));

        User user = userRepository.findById(reviewRecord.getUserId()).orElse(null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reviewId", reviewRecord.getId());
        data.put("contentType", reviewRecord.getContentType());
        data.put("contentId", reviewRecord.getContentId());

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("userId", reviewRecord.getUserId());
        userInfo.put("nickname", user != null ? user.getUsername() : "");
        data.put("userInfo", userInfo);

        data.put("submitTime", reviewRecord.getSubmitTime());
        return data;
    }

    @Transactional
    public Object submitReviewResult(Long adminUserId, Long reviewId, String result, String comment) {
        checkAdmin(adminUserId);
        ReviewRecord reviewRecord = reviewRecordRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(40401, "审核记录不存在"));

        reviewRecord.setReviewerId(adminUserId);
        reviewRecord.setResult(result);
        reviewRecord.setComment(comment);
        reviewRecord.setReviewTime(LocalDateTime.now());
        reviewRecordRepository.save(reviewRecord);
        contentReviewService.applyReviewResult(reviewRecord.getContentType(), reviewRecord.getContentId(), result);

        notificationService.createNotification(reviewRecord.getUserId(), "admin",
                "审核结果通知", "您的内容审核结果：" + result, reviewRecord.getContentType(), reviewRecord.getContentId());

        var resp = new LinkedHashMap<String, Object>();
        resp.put("reviewId", reviewRecord.getId());
        resp.put("result", result);
        resp.put("reviewTime", reviewRecord.getReviewTime());
        return resp;
    }

    @Transactional
    public Object banUser(Long adminUserId, Long targetUserId, String action, String reason, Integer duration) {
        checkAdmin(adminUserId);
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));

        user.setAccountStatus(action);
        userRepository.save(user);

        notificationService.createNotification(targetUserId, "admin",
                "账号状态变更", "您的账号已被" + action + "，原因：" + reason, null, null);

        var resp = new LinkedHashMap<String, Object>();
        resp.put("userId", targetUserId);
        resp.put("accountStatus", action);
        if (duration != null) {
            resp.put("mutedUntil", LocalDateTime.now().plusDays(duration));
        }
        return resp;
    }

    @Transactional
    public Object createFeedback(Long userId, String type, String content, String contact) {
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setType(type);
        feedback.setContent(content);
        feedback.setContact(contact);
        feedback.setFeedbackNumber("FB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", System.currentTimeMillis() % 1000));
        feedback.setStatus("PENDING");
        feedback = feedbackRepository.save(feedback);

        var resp = new LinkedHashMap<String, Object>();
        resp.put("feedbackNumber", feedback.getFeedbackNumber());
        return resp;
    }

    public PageResult<Map<String, Object>> listMyFeedback(Long userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Feedback> result = feedbackRepository.findMyFiltered(userId, status, pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (Feedback f : result.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("feedbackNumber", f.getFeedbackNumber());
            item.put("type", f.getType());
            item.put("content", f.getContent());
            item.put("status", f.getStatus());
            item.put("processComment", f.getProcessComment());
            item.put("createdAt", f.getCreatedAt());
            content.add(item);
        }
        return new PageResult<>(content, page, size, result.getTotalElements());
    }

    public PageResult<Map<String, Object>> listAllFeedback(Long adminUserId, String status, String type, int page, int size) {
        checkAdmin(adminUserId);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Feedback> result = feedbackRepository.findAllFiltered(status, type, pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (Feedback f : result.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("feedbackNumber", f.getFeedbackNumber());
            item.put("type", f.getType());
            item.put("content", f.getContent());
            item.put("status", f.getStatus());
            User user = userRepository.findById(f.getUserId()).orElse(null);
            Map<String, Object> userInfo = new LinkedHashMap<>();
            userInfo.put("userId", f.getUserId());
            userInfo.put("nickname", user != null ? user.getUsername() : "");
            item.put("userInfo", userInfo);
            item.put("createdAt", f.getCreatedAt());
            content.add(item);
        }
        return new PageResult<>(content, page, size, result.getTotalElements());
    }

    @Transactional
    public void processFeedback(Long adminUserId, String feedbackNumber, String status, String comment) {
        checkAdmin(adminUserId);
        Feedback feedback = feedbackRepository.findByFeedbackNumber(feedbackNumber)
                .orElseThrow(() -> new BusinessException(40401, "反馈不存在"));
        feedback.setStatus(status);
        feedback.setProcessComment(comment);
        feedback.setProcessedAt(LocalDateTime.now());
        feedbackRepository.save(feedback);
        notificationService.createNotification(feedback.getUserId(), "feedback",
                "反馈处理状态更新", "您的反馈处理状态已更新为：" + status,
                "feedback", feedback.getId());
    }
}
