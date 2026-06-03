package com.campushub.service;

import com.campushub.entity.ReviewRecord;
import com.campushub.repository.AdminRepository;
import com.campushub.repository.FeedbackRepository;
import com.campushub.repository.ReviewRecordRepository;
import com.campushub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private AdminRepository adminRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReviewRecordRepository reviewRecordRepository;
    @Mock private FeedbackRepository feedbackRepository;
    @Mock private NotificationService notificationService;
    @Mock private ContentReviewService contentReviewService;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(adminRepository, userRepository, reviewRecordRepository,
                feedbackRepository, notificationService, contentReviewService);
    }

    @Test
    void submitReviewResult_shouldApplyResultToReviewedContent() {
        when(adminRepository.existsByUserId(99L)).thenReturn(true);
        ReviewRecord record = new ReviewRecord();
        record.setId(1L);
        record.setContentType("treeholePost");
        record.setContentId(3L);
        record.setUserId(5L);
        record.setContentSnapshot("待审核树洞正文");
        when(reviewRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        Object result = adminService.submitReviewResult(99L, 1L, "PASSED", "内容合规");

        assertNotNull(result);
        verify(contentReviewService).applyReviewResult("treeholePost", 3L, "PASSED");
        verify(notificationService).createNotification(eq(5L), eq("admin"), anyString(), contains("已通过"),
                eq("treeholePost"), eq(3L));
    }

    @Test
    void getReviewDetail_shouldExposeContentSnapshotForReviewDecision() {
        when(adminRepository.existsByUserId(99L)).thenReturn(true);
        ReviewRecord record = new ReviewRecord();
        record.setId(1L);
        record.setContentType("loveReq");
        record.setContentId(7L);
        record.setUserId(5L);
        record.setContentSnapshot("希望认识喜欢散步和看电影的同学");
        when(reviewRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) adminService.getReviewDetail(99L, 1L);

        assertEquals("希望认识喜欢散步和看电影的同学", result.get("contentSnapshot"));
    }
}
