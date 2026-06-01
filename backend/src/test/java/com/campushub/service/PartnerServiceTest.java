package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.common.PageResult;
import com.campushub.dto.request.PartnerReqRequest;
import com.campushub.dto.request.ReviewSubmitRequest;
import com.campushub.entity.*;
import com.campushub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartnerServiceTest {

    @Mock private PartnerReqRepository partnerReqRepository;
    @Mock private PartnerMatchRepository partnerMatchRepository;
    @Mock private PartnerReviewRepository partnerReviewRepository;
    @Mock private UserCertRepository userCertRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ContentReviewService contentReviewService;

    private PartnerService partnerService;

    @BeforeEach
    void setUp() {
        partnerService = new PartnerService(partnerReqRepository, partnerMatchRepository,
                partnerReviewRepository, userCertRepository, userRepository, notificationService,
                contentReviewService);
    }

    @Test
    void createRequest_shouldRejectUncertified() {
        when(userCertRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

        PartnerReqRequest req = new PartnerReqRequest();
        req.setType("study");
        req.setDescription("找学习的搭子一起备考");

        assertThrows(BusinessException.class, () -> partnerService.createRequest(1L, req));
    }

    @Test
    void createRequest_shouldPublishImmediatelyWithCertifiedUser() {
        UserCert cert = new UserCert();
        cert.setCertStatus("CERTIFIED");
        when(userCertRepository.findByUserId(anyLong())).thenReturn(Optional.of(cert));

        PartnerReqRequest req = new PartnerReqRequest();
        req.setType("study");
        req.setDescription("自习");
        req.setValidDays(5);
        req.setMaxMembers(3);

        when(partnerReqRepository.save(any(PartnerReq.class))).thenAnswer(inv -> {
            PartnerReq r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        Object result = partnerService.createRequest(1L, req);
        assertNotNull(result);
        verify(partnerReqRepository).save(argThat(entity ->
                "PUBLISHED".equals(entity.getStatus()) && "{}".equals(entity.getConditions())));
        verify(contentReviewService, never()).submitForReview(anyString(), anyLong(), anyLong(), anyString());
    }

    @Test
    void applyMatch_shouldRejectSelfApply() {
        UserCert cert = new UserCert();
        cert.setCertStatus("CERTIFIED");
        when(userCertRepository.findByUserId(anyLong())).thenReturn(Optional.of(cert));

        PartnerReq req = new PartnerReq();
        req.setId(1L);
        req.setUserId(1L);
        req.setStatus("PUBLISHED");
        req.setExpireTime(LocalDateTime.now().plusDays(3));
        req.setMaxMembers(5);
        when(partnerReqRepository.findById(1L)).thenReturn(Optional.of(req));

        assertThrows(BusinessException.class, () -> partnerService.applyMatch(1L, 1L, ""));
    }

    @Test
    void applyMatch_shouldRejectExpiredRequest() {
        UserCert cert = new UserCert();
        cert.setCertStatus("CERTIFIED");
        when(userCertRepository.findByUserId(anyLong())).thenReturn(Optional.of(cert));

        PartnerReq req = new PartnerReq();
        req.setId(1L);
        req.setUserId(2L);
        req.setStatus("PUBLISHED");
        req.setExpireTime(LocalDateTime.now().minusDays(1));
        when(partnerReqRepository.findById(1L)).thenReturn(Optional.of(req));

        assertThrows(BusinessException.class, () -> partnerService.applyMatch(1L, 1L, ""));
    }

    @Test
    void applyMatch_shouldCreateReadableNotificationForPublisher() {
        UserCert cert = new UserCert();
        cert.setCertStatus("CERTIFIED");
        when(userCertRepository.findByUserId(20L)).thenReturn(Optional.of(cert));

        PartnerReq req = new PartnerReq();
        req.setId(1L);
        req.setUserId(10L);
        req.setType("sport");
        req.setDescription("晚上操场跑步");
        req.setStatus("PUBLISHED");
        req.setExpireTime(LocalDateTime.now().plusDays(3));
        req.setMaxMembers(2);
        when(partnerReqRepository.findById(1L)).thenReturn(Optional.of(req));
        when(partnerMatchRepository.countByRequestIdAndStatus(1L, "ACCEPTED")).thenReturn(0L);
        when(partnerMatchRepository.existsByRequestIdAndApplicantId(1L, 20L)).thenReturn(false);
        when(partnerMatchRepository.save(any(PartnerMatch.class))).thenAnswer(inv -> {
            PartnerMatch match = inv.getArgument(0);
            match.setId(3L);
            return match;
        });
        User applicant = new User();
        applicant.setUsername("小明同学");
        when(userRepository.findById(20L)).thenReturn(Optional.of(applicant));

        partnerService.applyMatch(20L, 1L, "我可以一起跑");

        verify(notificationService).createNotification(eq(10L), eq("partner_apply"), eq("新的搭子申请"),
                contains("小明同学"), eq("partnerMatch"), eq(3L));
        verify(notificationService).createNotification(eq(10L), eq("partner_apply"), eq("新的搭子申请"),
                contains("晚上操场跑步"), eq("partnerMatch"), eq(3L));
    }

    @Test
    void submitReview_shouldRejectDuplicateReview() {
        PartnerMatch match = new PartnerMatch();
        match.setId(1L);
        match.setRequestId(1L);
        match.setApplicantId(2L);
        match.setStatus("ENDED");
        when(partnerMatchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(partnerReviewRepository.existsByMatchIdAndReviewerId(1L, 1L)).thenReturn(true);

        ReviewSubmitRequest req = new ReviewSubmitRequest();
        req.setRating(5);
        req.setContent("非常靠谱的搭子，好评！");

        assertThrows(BusinessException.class, () -> partnerService.submitReview(1L, 1L, req));
    }

    @Test
    void updateMatchStatus_shouldAllowPublisherToAcceptPendingApply() {
        PartnerReq partnerReq = new PartnerReq();
        partnerReq.setId(1L);
        partnerReq.setUserId(10L);
        when(partnerReqRepository.findById(1L)).thenReturn(Optional.of(partnerReq));
        PartnerMatch match = new PartnerMatch();
        match.setId(2L);
        match.setRequestId(1L);
        match.setApplicantId(20L);
        match.setStatus("PENDING");
        when(partnerMatchRepository.findById(2L)).thenReturn(Optional.of(match));

        Object result = partnerService.updateMatchStatus(10L, 2L, "ACCEPTED", null);

        assertNotNull(result);
        assertEquals("ACCEPTED", match.getStatus());
        assertNotNull(match.getResponseTime());
    }

    @Test
    void listMatches_shouldReturnRequestsForPublisherToReview() {
        PartnerReq partnerReq = new PartnerReq();
        partnerReq.setId(1L);
        partnerReq.setUserId(10L);
        partnerReq.setType("study");
        partnerReq.setDescription("自习");
        when(partnerReqRepository.findById(1L)).thenReturn(Optional.of(partnerReq));

        PartnerMatch match = new PartnerMatch();
        match.setId(2L);
        match.setRequestId(1L);
        match.setApplicantId(20L);
        match.setStatus("PENDING");
        when(partnerMatchRepository.findByUserId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(match)));

        PageResult<java.util.Map<String, Object>> result = partnerService.listMatches(10L, "PENDING", 1, 20);

        assertEquals(1, result.getContent().size());
        assertEquals(2L, result.getContent().get(0).get("matchId"));
        assertEquals("PUBLISHER", result.getContent().get(0).get("myRole"));
    }

    @Test
    void listRequests_shouldReturnPublisherUserIdForOwnerActions() {
        PartnerReq req = new PartnerReq();
        req.setId(1L);
        req.setUserId(10L);
        req.setType("study");
        req.setDescription("自习");
        req.setStatus("PUBLISHED");
        req.setMaxMembers(2);
        req.setExpireTime(LocalDateTime.now().plusDays(2));
        when(partnerReqRepository.findPublicRequests(eq("study"), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(req), PageRequest.of(0, 20), 1));
        when(partnerMatchRepository.countByRequestIdAndStatus(1L, "ACCEPTED")).thenReturn(2L);

        User publisher = new User();
        publisher.setUsername("发布者");
        when(userRepository.findById(10L)).thenReturn(Optional.of(publisher));

        PageResult<java.util.Map<String, Object>> result = partnerService.listRequests("study", null, 1, 20, "publishTime");

        java.util.Map<String, Object> item = result.getContent().get(0);
        java.util.Map<String, Object> publisherInfo = (java.util.Map<String, Object>) item.get("publisherInfo");
        assertEquals(10L, publisherInfo.get("userId"));
        assertEquals("COMPLETED", item.get("status"));
    }

    @Test
    void cancelRequest_shouldRecordCancelInfoForPublisher() {
        PartnerReq req = new PartnerReq();
        req.setId(1L);
        req.setUserId(10L);
        req.setStatus("PUBLISHED");
        when(partnerReqRepository.findById(1L)).thenReturn(Optional.of(req));

        Object result = partnerService.cancelRequest(10L, 1L, "计划有变");

        assertNotNull(result);
        assertEquals("CANCELED", req.getStatus());
        assertTrue(req.getIsDeleted());
        assertEquals("计划有变", req.getCancelReason());
        assertNotNull(req.getCanceledAt());
        verify(partnerReqRepository).save(req);
    }

    @Test
    void cancelRequest_shouldRejectNonPublisher() {
        PartnerReq req = new PartnerReq();
        req.setId(1L);
        req.setUserId(10L);
        req.setStatus("PUBLISHED");
        when(partnerReqRepository.findById(1L)).thenReturn(Optional.of(req));

        assertThrows(BusinessException.class, () -> partnerService.cancelRequest(20L, 1L, ""));
    }

    @Test
    void submitReview_shouldRejectBeforeRelationshipEnded() {
        PartnerMatch match = new PartnerMatch();
        match.setId(1L);
        match.setRequestId(1L);
        match.setApplicantId(2L);
        match.setStatus("PENDING");
        when(partnerMatchRepository.findById(1L)).thenReturn(Optional.of(match));

        ReviewSubmitRequest req = new ReviewSubmitRequest();
        req.setRating(5);
        req.setContent("非常靠谱的搭子，好评！");

        assertThrows(BusinessException.class, () -> partnerService.submitReview(2L, 1L, req));
    }
}
