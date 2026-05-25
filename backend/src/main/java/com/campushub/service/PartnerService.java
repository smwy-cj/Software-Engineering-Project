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

    public PartnerService(PartnerReqRepository partnerReqRepository,
                          PartnerMatchRepository partnerMatchRepository,
                          PartnerReviewRepository partnerReviewRepository,
                          UserCertRepository userCertRepository,
                          UserRepository userRepository,
                          NotificationService notificationService) {
        this.partnerReqRepository = partnerReqRepository;
        this.partnerMatchRepository = partnerMatchRepository;
        this.partnerReviewRepository = partnerReviewRepository;
        this.userCertRepository = userCertRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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
        entity.setConditions(req.getConditions());
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
            item.put("validDays", req.getValidDays());
            item.put("createdAt", req.getCreatedAt());
            item.put("expireAt", req.getExpireTime());

            UserCert cert = userCertRepository.findByUserId(req.getUserId()).orElse(null);
            User user = userRepository.findById(req.getUserId()).orElse(null);
            Map<String, Object> publisherInfo = new LinkedHashMap<>();
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
    public Object applyMatch(Long userId, Long requestId, String message) {
        checkCertified(userId);

        PartnerReq req = partnerReqRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(40401, "搭子需求不存在"));

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

        notificationService.createNotification(req.getUserId(), "partner_apply",
                "新的搭子申请", "有人向你发起了搭子申请", "partnerMatch", match.getId());

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

    @Transactional
    public Object submitReview(Long userId, Long matchId, ReviewSubmitRequest req) {
        PartnerMatch match = partnerMatchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(40401, "匹配记录不存在"));

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
