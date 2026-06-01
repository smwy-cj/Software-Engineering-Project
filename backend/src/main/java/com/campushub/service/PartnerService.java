package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.common.PageResult;
import com.campushub.dto.request.PartnerReqRequest;
import com.campushub.dto.request.ReviewSubmitRequest;
import com.campushub.entity.*;
import com.campushub.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PartnerService {

    private final PartnerReqRepository partnerReqRepository;
    private final PartnerMatchRepository partnerMatchRepository;
    private final PartnerReviewRepository partnerReviewRepository;
    private final UserCertRepository userCertRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ContentReviewService contentReviewService;

    public PartnerService(PartnerReqRepository partnerReqRepository,
                          PartnerMatchRepository partnerMatchRepository,
                           PartnerReviewRepository partnerReviewRepository,
                           UserCertRepository userCertRepository,
                           UserRepository userRepository,
                           NotificationService notificationService,
                           ContentReviewService contentReviewService) {
        this.partnerReqRepository = partnerReqRepository;
        this.partnerMatchRepository = partnerMatchRepository;
        this.partnerReviewRepository = partnerReviewRepository;
        this.userCertRepository = userCertRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.contentReviewService = contentReviewService;
    }

    private void checkCertified(Long userId) {
        UserCert cert = userCertRepository.findByUserId(userId).orElse(null);
        if (cert == null || !"CERTIFIED".equals(cert.getCertStatus())) {
            throw new BusinessException(40302, "请先完成实名认证后再发布搭子需求");
        }
    }

    @Transactional
    public Object createRequest(Long userId, PartnerReqRequest req) {
        checkCertified(userId);

        PartnerReq entity = new PartnerReq();
        entity.setUserId(userId);
        entity.setType(req.getType());
        entity.setDescription(req.getDescription());
        entity.setConditions(req.getConditions() != null && !req.getConditions().isBlank() ? req.getConditions() : "{}");
        entity.setValidDays(Math.max(1, Math.min(7, req.getValidDays())));
        entity.setMaxMembers(Math.max(1, Math.min(10, req.getMaxMembers())));
        entity.setVisibility(req.getVisibility() != null ? req.getVisibility() : "sameSchool");
        entity.setStatus("PUBLISHED");
        entity.setExpireTime(LocalDateTime.now().plusDays(entity.getValidDays()));
        entity = partnerReqRepository.save(entity);

        var resp = new LinkedHashMap<String, Object>();
        resp.put("requestId", entity.getId());
        resp.put("status", entity.getStatus());
        resp.put("createdAt", entity.getCreatedAt());
        resp.put("expireAt", entity.getExpireTime());
        return resp;
    }

    public PageResult<Map<String, Object>> listRequests(String type, String keyword, int page, int size, String sortBy) {
        Sort sort = Sort.by("hot".equals(sortBy) ? Sort.Direction.DESC : Sort.Direction.DESC,
                "hot".equals(sortBy) ? "likeCount" : "createdAt");
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<PartnerReq> result = partnerReqRepository.findPublicRequests(type, keyword, pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (PartnerReq req : result.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("requestId", req.getId());
            item.put("type", req.getType());
            item.put("description", req.getDescription());
            item.put("maxMembers", req.getMaxMembers());
            long matchCount = partnerMatchRepository.countByRequestIdAndStatus(req.getId(), "ACCEPTED");
            item.put("currentMatches", matchCount);
            item.put("status", matchCount >= req.getMaxMembers() ? "COMPLETED" : req.getStatus());
            item.put("validDays", req.getValidDays());
            item.put("createdAt", req.getCreatedAt());
            item.put("expireAt", req.getExpireTime());

            UserCert cert = userCertRepository.findByUserId(req.getUserId()).orElse(null);
            User user = userRepository.findById(req.getUserId()).orElse(null);
            Map<String, Object> publisherInfo = new LinkedHashMap<>();
            publisherInfo.put("userId", req.getUserId());
            publisherInfo.put("nickname", user != null ? user.getUsername() : "未知");
            publisherInfo.put("grade", cert != null ? cert.getGrade() : "");
            publisherInfo.put("major", cert != null ? cert.getMajor() : "");
            publisherInfo.put("avatar", user != null ? user.getAvatar() : null);
            item.put("publisherInfo", publisherInfo);

            content.add(item);
        }

        return new PageResult<>(content, page, size, result.getTotalElements());
    }

    @Transactional
    public Object cancelRequest(Long userId, Long requestId, String reason) {
        PartnerReq req = partnerReqRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(40401, "搭子需求不存在"));
        if (!req.getUserId().equals(userId)) {
            throw new BusinessException(40301, "只有发布者可以撤销该搭子需求");
        }
        if (!"PUBLISHED".equals(req.getStatus())) {
            throw new BusinessException(40003, "当前搭子需求不可撤销");
        }

        req.setStatus("CANCELED");
        req.setIsDeleted(true);
        req.setCanceledAt(LocalDateTime.now());
        req.setCancelReason(reason != null && !reason.isBlank() ? reason : "发布者撤销");
        partnerReqRepository.save(req);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestId", req.getId());
        data.put("status", req.getStatus());
        data.put("canceledAt", req.getCanceledAt());
        data.put("cancelReason", req.getCancelReason());
        return data;
    }

    @Transactional
    public Object applyMatch(Long userId, Long requestId, String message) {
        checkCertified(userId);

        PartnerReq req = partnerReqRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(40401, "搭子需求不存在"));
        if (!"PUBLISHED".equals(req.getStatus())) {
            throw new BusinessException(40003, "该搭子需求尚未发布，无法申请");
        }

        if (req.getUserId().equals(userId)) {
            throw new BusinessException(40003, "不能向自己发布的搭子需求发起申请");
        }
        if (req.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(40003, "该搭子需求已过期，无法申请");
        }
        long acceptedCount = partnerMatchRepository.countByRequestIdAndStatus(requestId, "ACCEPTED");
        if (acceptedCount >= req.getMaxMembers()) {
            throw new BusinessException(40003, "该搭子需求匹配人数已满");
        }
        if (partnerMatchRepository.existsByRequestIdAndApplicantId(requestId, userId)) {
            throw new BusinessException(40003, "您已申请过该需求，请勿重复申请");
        }

        PartnerMatch match = new PartnerMatch();
        match.setRequestId(requestId);
        match.setApplicantId(userId);
        match.setApplyMessage(message);
        match.setStatus("PENDING");
        match = partnerMatchRepository.save(match);

        User applicant = userRepository.findById(userId).orElse(null);
        String applicantName = applicant != null && applicant.getUsername() != null && !applicant.getUsername().isBlank()
                ? applicant.getUsername()
                : "同学";
        String requestSummary = req.getDescription() != null && req.getDescription().length() > 36
                ? req.getDescription().substring(0, 36) + "..."
                : req.getDescription();
        String notificationContent = applicantName + "申请加入你的搭子需求：" + requestSummary;
        if (message != null && !message.isBlank()) {
            notificationContent += "。附言：" + message;
        }
        if (notificationContent.length() > 240) {
            notificationContent = notificationContent.substring(0, 240) + "...";
        }

        notificationService.createNotification(req.getUserId(), "partner_apply",
                "新的搭子申请", notificationContent, "partnerMatch", match.getId());

        var resp = new LinkedHashMap<String, Object>();
        resp.put("matchId", match.getId());
        resp.put("requestId", requestId);
        resp.put("status", match.getStatus());
        resp.put("applyTime", match.getApplyTime());
        return resp;
    }

    public Object getMatchDetail(Long userId, Long matchId) {
        PartnerMatch match = partnerMatchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(40401, "匹配记录不存在"));

        PartnerReq req = partnerReqRepository.findById(match.getRequestId()).orElse(null);
        if (req == null) return null;

        if (!match.getApplicantId().equals(userId) && !req.getUserId().equals(userId)) {
            throw new BusinessException(40301, "您不是该匹配的参与方，无权查看详情");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("matchId", match.getId());
        data.put("status", match.getStatus());

        Map<String, Object> reqData = new LinkedHashMap<>();
        reqData.put("requestId", req.getId());
        reqData.put("type", req.getType());
        reqData.put("description", req.getDescription());
        data.put("request", reqData);

        User publisher = userRepository.findById(req.getUserId()).orElse(null);
        UserCert pubCert = userCertRepository.findByUserId(req.getUserId()).orElse(null);
        Map<String, Object> pubInfo = new LinkedHashMap<>();
        pubInfo.put("userId", req.getUserId());
        pubInfo.put("nickname", publisher != null ? publisher.getUsername() : "");
        pubInfo.put("grade", pubCert != null ? pubCert.getGrade() : "");
        pubInfo.put("major", pubCert != null ? pubCert.getMajor() : "");
        data.put("publisher", pubInfo);

        User applicant = userRepository.findById(match.getApplicantId()).orElse(null);
        UserCert appCert = userCertRepository.findByUserId(match.getApplicantId()).orElse(null);
        Map<String, Object> appInfo = new LinkedHashMap<>();
        appInfo.put("userId", match.getApplicantId());
        appInfo.put("nickname", applicant != null ? applicant.getUsername() : "");
        appInfo.put("grade", appCert != null ? appCert.getGrade() : "");
        appInfo.put("major", appCert != null ? appCert.getMajor() : "");
        data.put("applicant", appInfo);

        data.put("applyMessage", match.getApplyMessage());
        data.put("applyTime", match.getApplyTime());
        data.put("responseTime", match.getResponseTime());
        data.put("canChat", "ACCEPTED".equals(match.getStatus()));

        return data;
    }

    public PageResult<Map<String, Object>> listMatches(Long userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "applyTime"));
        Page<PartnerMatch> result = partnerMatchRepository.findByUserId(userId, pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (PartnerMatch match : result.getContent()) {
            if (status != null && !status.equals(match.getStatus())) {
                continue;
            }
            PartnerReq req = partnerReqRepository.findById(match.getRequestId()).orElse(null);
            if (req == null) {
                continue;
            }

            boolean publisher = req.getUserId().equals(userId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("matchId", match.getId());
            item.put("requestId", req.getId());
            item.put("status", match.getStatus());
            item.put("myRole", publisher ? "PUBLISHER" : "APPLICANT");
            item.put("applyMessage", match.getApplyMessage());
            item.put("applyTime", match.getApplyTime());
            item.put("responseTime", match.getResponseTime());

            Map<String, Object> reqData = new LinkedHashMap<>();
            reqData.put("type", req.getType());
            reqData.put("description", req.getDescription());
            item.put("request", reqData);

            Long otherUserId = publisher ? match.getApplicantId() : req.getUserId();
            User otherUser = userRepository.findById(otherUserId).orElse(null);
            UserCert otherCert = userCertRepository.findByUserId(otherUserId).orElse(null);
            Map<String, Object> other = new LinkedHashMap<>();
            other.put("userId", otherUserId);
            other.put("nickname", otherUser != null ? otherUser.getUsername() : "同学");
            other.put("grade", otherCert != null ? otherCert.getGrade() : "");
            other.put("major", otherCert != null ? otherCert.getMajor() : "");
            item.put("otherUser", other);

            content.add(item);
        }
        return new PageResult<>(content, page, size, result.getTotalElements());
    }

    @Transactional
    public Object updateMatchStatus(Long userId, Long matchId, String status, String reason) {
        PartnerMatch match = partnerMatchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(40401, "匹配记录不存在"));
        PartnerReq req = partnerReqRepository.findById(match.getRequestId())
                .orElseThrow(() -> new BusinessException(40401, "搭子需求不存在"));

        boolean publisher = req.getUserId().equals(userId);
        boolean applicant = match.getApplicantId().equals(userId);
        if (!publisher && !applicant) {
            throw new BusinessException(40301, "您不是该匹配的参与方，无权操作");
        }
        if (!Set.of("ACCEPTED", "REJECTED", "CANCELED", "ENDED").contains(status)) {
            throw new BusinessException(40003, "不支持的匹配状态");
        }
        if (Set.of("ACCEPTED", "REJECTED").contains(status) && !publisher) {
            throw new BusinessException(40301, "只有发布者可以处理匹配申请");
        }
        if ("CANCELED".equals(status) && !applicant) {
            throw new BusinessException(40301, "只有申请者可以取消匹配申请");
        }
        if ("ENDED".equals(status) && !"ACCEPTED".equals(match.getStatus())) {
            throw new BusinessException(40003, "只有已建立的搭子关系可以结束");
        }
        if (!"PENDING".equals(match.getStatus()) && !"ENDED".equals(status)) {
            throw new BusinessException(40003, "当前匹配状态不可变更");
        }

        match.setStatus(status);
        if (Set.of("ACCEPTED", "REJECTED", "CANCELED").contains(status)) {
            match.setResponseTime(LocalDateTime.now());
        }
        if ("ENDED".equals(status)) {
            match.setEndTime(LocalDateTime.now());
            match.setEndReason(reason);
        }
        partnerMatchRepository.save(match);

        Long notifyUserId = publisher ? match.getApplicantId() : req.getUserId();
        notificationService.createNotification(notifyUserId, "partner_match",
                "搭子匹配状态更新", "搭子匹配状态已更新为：" + status, "partnerMatch", match.getId());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("matchId", match.getId());
        data.put("status", match.getStatus());
        data.put("responseTime", match.getResponseTime());
        data.put("endTime", match.getEndTime());
        return data;
    }

    @Transactional
    public Object submitReview(Long userId, Long matchId, ReviewSubmitRequest req) {
        PartnerMatch match = partnerMatchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(40401, "匹配记录不存在"));
        if (!"ENDED".equals(match.getStatus())) {
            throw new BusinessException(40003, "搭子关系结束后才能评价");
        }

        if (partnerReviewRepository.existsByMatchIdAndReviewerId(matchId, userId)) {
            throw new BusinessException(40003, "您已经对该搭子进行过评价");
        }

        PartnerReq partnerReq = partnerReqRepository.findById(match.getRequestId()).orElse(null);
        if (partnerReq == null) return null;

        Long targetId = match.getApplicantId().equals(userId) ? partnerReq.getUserId() : match.getApplicantId();

        PartnerReview review = new PartnerReview();
        review.setMatchId(matchId);
        review.setReviewerId(userId);
        review.setTargetId(targetId);
        review.setRating(req.getRating());
        review.setContent(req.getContent());
        review = partnerReviewRepository.save(review);

        var resp = new LinkedHashMap<String, Object>();
        resp.put("reviewId", review.getId());
        resp.put("rating", review.getRating());
        resp.put("content", review.getContent());
        resp.put("createTime", review.getCreatedAt());
        return resp;
    }
}
