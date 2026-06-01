package com.campushub.service;

import com.campushub.common.PageResult;
import com.campushub.entity.LoveMatch;
import com.campushub.entity.LoveReq;
import com.campushub.entity.UserCert;
import org.springframework.data.domain.Sort;
import com.campushub.repository.LoveMatchRepository;
import com.campushub.repository.LoveReqRepository;
import com.campushub.repository.UserCertRepository;
import com.campushub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoveServiceTest {

    @Mock private LoveReqRepository loveReqRepository;
    @Mock private LoveMatchRepository loveMatchRepository;
    @Mock private UserCertRepository userCertRepository;
    @Mock private UserRepository userRepository;
    @Mock private ContentReviewService contentReviewService;
    @Mock private NotificationService notificationService;

    private LoveService loveService;

    @BeforeEach
    void setUp() {
        loveService = new LoveService(loveReqRepository, loveMatchRepository,
                userCertRepository, userRepository, contentReviewService, notificationService);
    }

    @Test
    void createLoveRequest_shouldCreatePendingReviewWhenUserCertified() {
        UserCert cert = new UserCert();
        cert.setCertStatus("CERTIFIED");
        when(userCertRepository.findByUserId(1L)).thenReturn(Optional.of(cert));
        when(loveReqRepository.save(any(LoveReq.class))).thenAnswer(inv -> {
            LoveReq req = inv.getArgument(0);
            req.setId(8L);
            return req;
        });

        Object result = loveService.createLoveRequest(1L, "希望认识一起学习和运动的朋友", 7, "sameSchool");

        assertNotNull(result);
        verify(loveReqRepository).save(argThat(req ->
                req.getProfileId() == null && "PENDING".equals(req.getStatus())));
        verify(contentReviewService).submitForReview("loveReq", 8L, 1L, "希望认识一起学习和运动的朋友");
    }

    @Test
    void sendHeart_shouldAcceptBothMatchesWhenReverseHeartExists() {
        UserCert cert = new UserCert();
        cert.setCertStatus("CERTIFIED");
        when(userCertRepository.findByUserId(1L)).thenReturn(Optional.of(cert));

        LoveReq targetRequest = new LoveReq();
        targetRequest.setId(10L);
        targetRequest.setUserId(2L);
        when(loveReqRepository.findById(10L)).thenReturn(Optional.of(targetRequest));

        LoveReq myRequest = new LoveReq();
        myRequest.setId(11L);
        myRequest.setUserId(1L);
        when(loveReqRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(1L, "PUBLISHED"))
                .thenReturn(Optional.of(myRequest));

        LoveMatch reverse = new LoveMatch();
        reverse.setId(7L);
        reverse.setRequestId(11L);
        reverse.setApplicantId(2L);
        reverse.setStatus("PENDING");
        when(loveMatchRepository.findByRequestIdAndApplicantId(11L, 2L)).thenReturn(Optional.of(reverse));
        when(loveMatchRepository.findByRequestIdAndApplicantId(10L, 1L)).thenReturn(Optional.empty());
        when(loveMatchRepository.save(any(LoveMatch.class))).thenAnswer(inv -> {
            LoveMatch match = inv.getArgument(0);
            match.setId(12L);
            return match;
        });

        Object result = loveService.sendHeart(1L, 10L);

        assertNotNull(result);
        assertEquals("ACCEPTED", reverse.getStatus());
        verify(loveMatchRepository, times(2)).save(argThat(match -> "ACCEPTED".equals(match.getStatus())));
    }

    @Test
    void listLoveRequests_shouldOrderInteractionTabByRecentMatches() {
        LoveReq olderWithoutInteraction = loveReq(1L, 1L, "希望认识一起逛展的同学");
        LoveReq activeRequest = loveReq(2L, 2L, "想找周末一起喝咖啡的人");
        when(loveReqRepository.findByStatus(eq("PUBLISHED"), any(Sort.class)))
                .thenReturn(List.of(olderWithoutInteraction, activeRequest));

        LoveMatch recentInteraction = new LoveMatch();
        recentInteraction.setRequestId(2L);
        when(loveMatchRepository.findTop100ByOrderByApplyTimeDesc()).thenReturn(List.of(recentInteraction));

        PageResult<Map<String, Object>> result = loveService.listLoveRequests("interaction", 1, 20);

        assertEquals(2, result.getContent().size());
        assertEquals(2L, result.getContent().get(0).get("requestId"));
        assertEquals(1L, result.getContent().get(1).get("requestId"));
    }

    private LoveReq loveReq(Long id, Long userId, String description) {
        LoveReq req = new LoveReq();
        req.setId(id);
        req.setUserId(userId);
        req.setDescription(description);
        req.setStatus("PUBLISHED");
        req.setValidDays(7);
        req.setScope("sameSchool");
        return req;
    }
}
