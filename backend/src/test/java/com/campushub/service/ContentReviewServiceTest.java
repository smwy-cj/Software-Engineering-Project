package com.campushub.service;

import com.campushub.entity.ReviewRecord;
import com.campushub.repository.LoveReqRepository;
import com.campushub.repository.PartnerReqRepository;
import com.campushub.repository.ReviewRecordRepository;
import com.campushub.repository.SensitiveWordRepository;
import com.campushub.repository.TreeHolePostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentReviewServiceTest {

    @Mock private ReviewRecordRepository reviewRecordRepository;
    @Mock private SensitiveWordRepository sensitiveWordRepository;
    @Mock private TreeHolePostRepository treeHolePostRepository;
    @Mock private PartnerReqRepository partnerReqRepository;
    @Mock private LoveReqRepository loveReqRepository;

    private ContentReviewService contentReviewService;

    @BeforeEach
    void setUp() {
        contentReviewService = new ContentReviewService(reviewRecordRepository, sensitiveWordRepository,
                treeHolePostRepository, partnerReqRepository, loveReqRepository);
    }

    @Test
    void submitForReview_shouldStoreTrimmedContentSnapshot() {
        when(sensitiveWordRepository.findAll()).thenReturn(List.of());
        when(reviewRecordRepository.save(any(ReviewRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewRecord record = contentReviewService.submitForReview("loveReq", 7L, 5L,
                "  希望认识喜欢散步和看电影的同学  ");

        assertEquals("希望认识喜欢散步和看电影的同学", record.getContentSnapshot());
    }
}
