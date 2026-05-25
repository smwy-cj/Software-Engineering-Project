package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.common.PageResult;
import com.campushub.dto.request.LoveProfileRequest;
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

    private final LoveProfileRepository loveProfileRepository;
    private final LoveReqRepository loveReqRepository;
    private final LoveMatchRepository loveMatchRepository;
    private final UserCertRepository userCertRepository;
    private final UserRepository userRepository;

    public LoveService(LoveProfileRepository loveProfileRepository,
                       LoveReqRepository loveReqRepository,
                       LoveMatchRepository loveMatchRepository,
                       UserCertRepository userCertRepository,
                       UserRepository userRepository) {
        this.loveProfileRepository = loveProfileRepository;
        this.loveReqRepository = loveReqRepository;
        this.loveMatchRepository = loveMatchRepository;
        this.userCertRepository = userCertRepository;
        this.userRepository = userRepository;
    }

    private void checkCertified(Long userId) {
        UserCert cert = userCertRepository.findByUserId(userId).orElse(null);
        if (cert == null || !"CERTIFIED".equals(cert.getCertStatus())) {
            throw new BusinessException(40302, "请先完成实名认证");
        }
    }

    @Transactional
    public Object updateProfile(Long userId, LoveProfileRequest req) {
        checkCertified(userId);

        LoveProfile profile = loveProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    LoveProfile p = new LoveProfile();
                    p.setUserId(userId);
                    return p;
                });

        profile.setGender(req.getGender());
        profile.setAge(req.getAge());
        profile.setHeight(req.getHeight());
        profile.setWeight(req.getWeight());
        profile.setConstellation(req.getConstellation());
        profile.setInterests(req.getInterests());
        profile.setMatePreference(req.getMatePreference());
        profile.setDeclaration(req.getDeclaration());
        profile.setVisibility(req.getVisibility() != null ? req.getVisibility() : "all");

        int completeness = 0;
        if (req.getGender() != null) completeness += 20;
        if (req.getAge() > 0) completeness += 15;
        if (req.getHeight() != null) completeness += 10;
        if (req.getInterests() != null) completeness += 15;
        if (req.getMatePreference() != null) completeness += 20;
        if (req.getDeclaration() != null) completeness += 20;
        profile.setCompleteness(Math.min(completeness, 100));

        profile = loveProfileRepository.save(profile);

        var resp = new LinkedHashMap<String, Object>();
        resp.put("profileId", profile.getId());
        resp.put("completeness", profile.getCompleteness());
        return resp;
    }

    public PageResult<Map<String, Object>> listProfiles(String gender, Integer minAge, Integer maxAge,
                                                         int page, int size, String sortBy) {
        Sort sort = Sort.by("matchCount".equals(sortBy) ? Sort.Direction.DESC : Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<LoveProfile> result = loveProfileRepository.findPublicProfiles(gender, minAge, maxAge, pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (LoveProfile lp : result.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            User user = userRepository.findById(lp.getUserId()).orElse(null);
            UserCert cert = userCertRepository.findByUserId(lp.getUserId()).orElse(null);
            item.put("userId", lp.getUserId());
            item.put("nickname", user != null ? user.getUsername() : "");
            item.put("age", lp.getAge());
            item.put("university", cert != null ? cert.getUniversity() : "");
            item.put("major", cert != null ? cert.getMajor() : "");
            item.put("interests", lp.getInterests());
            item.put("declaration", lp.getDeclaration());
            item.put("photos", lp.getPhotos());
            item.put("matchCount", 0);
            content.add(item);
        }
        return new PageResult<>(content, page, size, result.getTotalElements());
    }

    @Transactional
    public Object createLoveRequest(Long userId, String description, int validDays, String scope) {
        checkCertified(userId);

        LoveProfile profile = loveProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(40003, "请先完善交友资料"));

        if (profile.getCompleteness() < 80) {
            throw new BusinessException(40003, "交友资料完整度需达到80%才能发布需求");
        }

        LoveReq req = new LoveReq();
        req.setUserId(userId);
        req.setProfileId(profile.getId());
        req.setDescription(description);
        req.setValidDays(Math.max(1, Math.min(14, validDays)));
        req.setScope(scope != null ? scope : "sameSchool");
        req.setStatus("PUBLISHED");
        req.setExpireTime(LocalDateTime.now().plusDays(req.getValidDays()));
        req = loveReqRepository.save(req);

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

        LoveMatch match = new LoveMatch();
        match.setRequestId(requestId);
        match.setApplicantId(userId);
        match.setStatus("PENDING");
        match = loveMatchRepository.save(match);

        var resp = new LinkedHashMap<String, Object>();
        resp.put("matchId", match.getId());
        resp.put("status", "PENDING");
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
}
