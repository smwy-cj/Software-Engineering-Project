package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.entity.ReviewRecord;
import com.campushub.entity.SensitiveWord;
import com.campushub.repository.LoveReqRepository;
import com.campushub.repository.PartnerReqRepository;
import com.campushub.repository.ReviewRecordRepository;
import com.campushub.repository.SensitiveWordRepository;
import com.campushub.repository.TreeHolePostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ContentReviewService {

    private final ReviewRecordRepository reviewRecordRepository;
    private final SensitiveWordRepository sensitiveWordRepository;
    private final TreeHolePostRepository treeHolePostRepository;
    private final PartnerReqRepository partnerReqRepository;
    private final LoveReqRepository loveReqRepository;

    public ContentReviewService(ReviewRecordRepository reviewRecordRepository,
                                SensitiveWordRepository sensitiveWordRepository,
                                TreeHolePostRepository treeHolePostRepository,
                                PartnerReqRepository partnerReqRepository,
                                LoveReqRepository loveReqRepository) {
        this.reviewRecordRepository = reviewRecordRepository;
        this.sensitiveWordRepository = sensitiveWordRepository;
        this.treeHolePostRepository = treeHolePostRepository;
        this.partnerReqRepository = partnerReqRepository;
        this.loveReqRepository = loveReqRepository;
    }

    @Transactional
    public ReviewRecord submitForReview(String contentType, Long contentId, Long userId, String text) {
        rejectSensitiveText(text);

        ReviewRecord record = new ReviewRecord();
        record.setContentType(contentType);
        record.setContentId(contentId);
        record.setUserId(userId);
        record.setResult("PENDING");
        record.setComment("待人工审核");
        record.setSubmitTime(LocalDateTime.now());
        return reviewRecordRepository.save(record);
    }

    @Transactional
    public void applyReviewResult(String contentType, Long contentId, String result) {
        String status = switch (result) {
            case "PASSED", "WARNING" -> "PUBLISHED";
            case "REJECTED" -> "REJECTED";
            default -> throw new BusinessException(40003, "不支持的审核结果");
        };

        switch (contentType) {
            case "treeholePost" -> treeHolePostRepository.findById(contentId).ifPresent(post -> {
                post.setStatus(status);
                treeHolePostRepository.save(post);
            });
            case "partnerReq" -> partnerReqRepository.findById(contentId).ifPresent(req -> {
                req.setStatus(status);
                partnerReqRepository.save(req);
            });
            case "loveReq" -> loveReqRepository.findById(contentId).ifPresent(req -> {
                req.setStatus(status);
                loveReqRepository.save(req);
            });
            default -> throw new BusinessException(40003, "不支持的审核内容类型");
        }
    }

    private void rejectSensitiveText(String text) {
        if (text == null) {
            return;
        }
        String normalized = text.toLowerCase();
        for (SensitiveWord word : sensitiveWordRepository.findAll()) {
            if (word.getWord() != null && !word.getWord().isBlank()
                    && normalized.contains(word.getWord().toLowerCase())) {
                throw new BusinessException(42201, "内容包含违规信息，请修改后重新发布");
            }
        }
    }
}
