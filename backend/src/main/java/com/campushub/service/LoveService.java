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
import java.util.*;

@Service
public class LoveService {

    private final LoveReqRepository loveReqRepository;
    private final LoveMatchRepository loveMatchRepository;
    private final UserCertRepository userCertRepository;
    private final UserRepository userRepository;
    private final ContentReviewService contentReviewService;
    private final NotificationService notificationService;

    public LoveService(LoveReqRepository loveReqRepository,
                       LoveMatchRepository loveMatchRepository,
                       UserCertRepository userCertRepository,
                       UserRepository userRepository,
                       ContentReviewService contentReviewService,
                       NotificationService notificationService) {
        this.loveReqRepository = loveReqRepository;
        this.loveMatchRepository = loveMatchRepository;
        this.userCertRepository = userCertRepository;
        this.userRepository = userRepository;
        this.contentReviewService = contentReviewService;
        this.notificationService = notificationService;
    }

    private void checkCertified(Long userId) {
        UserCert cert = userCertRepository.findByUserId(userId).orElse(null);
        if (cert == null || !"CERTIFIED".equals(cert.getCertStatus())) {
            throw new BusinessException(40302, "请先完成实名认证");
        }
    }

    public PageResult<Map<String, Object>> listLoveRequests(String sortBy, int page, int size) {
        List<LoveReq> published = new ArrayList<>(loveReqRepository.findByStatus(
                "PUBLISHED", Sort.by(Sort.Direction.DESC, "createdAt")));

        if ("interaction".equals(sortBy)) {
            Map<Long, Integer> interactionRank = new HashMap<>();
            int rank = 0;
            for (LoveMatch match : loveMatchRepository.findTop100ByOrderByApplyTimeDesc()) {
                interactionRank.putIfAbsent(match.getRequestId(), rank++);
            }
            published.sort(Comparator
                    .comparingInt((LoveReq req) -> interactionRank.getOrDefault(req.getId(), Integer.MAX_VALUE))
                    .thenComparing(LoveReq::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(50, size));
        int fromIndex = Math.min((safePage - 1) * safeSize, published.size());
        int toIndex = Math.min(fromIndex + safeSize, published.size());

        List<Map<String, Object>> content = new ArrayList<>();
        for (LoveReq req : published.subList(fromIndex, toIndex)) {
            content.add(toLoveRequestItem(req));
        }
        return new PageResult<>(content, safePage, safeSize, published.size());
    }

    @Transactional
    public Object createLoveRequest(Long userId, String description, int validDays, String scope) {
        checkCertified(userId);

        LoveReq req = new LoveReq();
        req.setUserId(userId);
        req.setProfileId(null);
        req.setDescription(description);
        req.setValidDays(Math.max(1, Math.min(14, validDays)));
        req.setScope(scope != null ? scope : "sameSchool");
        req.setStatus("PENDING");
        req.setExpireTime(LocalDateTime.now().plusDays(req.getValidDays()));
        req = loveReqRepository.save(req);
        contentReviewService.submitForReview("loveReq", req.getId(), userId, description);

        var resp = new LinkedHashMap<String, Object>();
        resp.put("requestId", req.getId());
        resp.put("status", req.getStatus());
        resp.put("createdAt", req.getCreatedAt());
        resp.put("expireAt", req.getExpireTime());
        return resp;
    }

    @Transactional
    public Object sendHeart(Long userId, Long requestId) {
        checkCertified(userId);

        LoveReq req = loveReqRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(40401, "交友需求不存在"));

        if (req.getUserId().equals(userId)) {
            throw new BusinessException(40003, "不能对自己发布的需求发送心动");
        }

        if (loveMatchRepository.findByRequestIdAndApplicantId(requestId, userId).isPresent()) {
            throw new BusinessException(40003, "您已发送过心动");
        }

        Optional<LoveReq> myPublishedReq = loveReqRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, "PUBLISHED");
        Optional<LoveMatch> reverseMatch = myPublishedReq
                .flatMap(myReq -> loveMatchRepository.findByRequestIdAndApplicantId(myReq.getId(), req.getUserId()));
        boolean accepted = reverseMatch.isPresent();

        LoveMatch match = new LoveMatch();
        match.setRequestId(requestId);
        match.setApplicantId(userId);
        match.setStatus(accepted ? "ACCEPTED" : "PENDING");
        if (accepted) {
            match.setResponseTime(LocalDateTime.now());
            reverseMatch.get().setStatus("ACCEPTED");
            reverseMatch.get().setResponseTime(LocalDateTime.now());
            loveMatchRepository.save(reverseMatch.get());
        }
        match = loveMatchRepository.save(match);
        if (accepted) {
            notificationService.createNotification(req.getUserId(), "love_match",
                    "双向心动匹配成功", "你们已双向心动，可以开始交流", "loveMatch", match.getId());
            notificationService.createNotification(userId, "love_match",
                    "双向心动匹配成功", "你们已双向心动，可以开始交流", "loveMatch", match.getId());
        }

        var resp = new LinkedHashMap<String, Object>();
        resp.put("matchId", match.getId());
        resp.put("status", match.getStatus());
        return resp;
    }

    public PageResult<Map<String, Object>> listMatches(Long userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "applyTime"));
        Page<LoveMatch> result = loveMatchRepository.findByUserId(userId, pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (LoveMatch m : result.getContent()) {
            if (status != null && !status.equals(m.getStatus())) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("matchId", m.getId());
            item.put("status", m.getStatus());
            Long partnerId = m.getApplicantId().equals(userId) ?
                    loveReqRepository.findById(m.getRequestId()).map(LoveReq::getUserId).orElse(null) :
                    m.getApplicantId();
            User partner = partnerId != null ? userRepository.findById(partnerId).orElse(null) : null;
            Map<String, Object> partnerInfo = new LinkedHashMap<>();
            partnerInfo.put("userId", partnerId);
            partnerInfo.put("nickname", partner != null ? partner.getUsername() : "");
            partnerInfo.put("avatar", partner != null ? partner.getAvatar() : null);
            item.put("partner", partnerInfo);
            item.put("createdAt", m.getApplyTime());
            content.add(item);
        }
        return new PageResult<>(content, page, size, result.getTotalElements());
    }

    private Map<String, Object> toLoveRequestItem(LoveReq req) {
        Map<String, Object> item = new LinkedHashMap<>();
        User user = userRepository.findById(req.getUserId()).orElse(null);
        UserCert cert = userCertRepository.findByUserId(req.getUserId()).orElse(null);

        item.put("requestId", req.getId());
        item.put("description", req.getDescription());
        item.put("scope", req.getScope());
        item.put("status", req.getStatus());
        item.put("validDays", req.getValidDays());
        item.put("createdAt", req.getCreatedAt());
        item.put("expireAt", req.getExpireTime());
        Map<String, Object> publisherInfo = new LinkedHashMap<>();
        publisherInfo.put("userId", req.getUserId());
        publisherInfo.put("nickname", user != null ? user.getUsername() : "同学");
        publisherInfo.put("avatar", user != null && user.getAvatar() != null ? user.getAvatar() : "");
        publisherInfo.put("gender", cert != null ? cert.getGender() : "");
        publisherInfo.put("age", cert != null ? cert.getAge() : 0);
        publisherInfo.put("major", cert != null ? cert.getMajor() : "");
        publisherInfo.put("university", cert != null ? cert.getUniversity() : "");
        item.put("publisherInfo", publisherInfo);
        return item;
    }
}
