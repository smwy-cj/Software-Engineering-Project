package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.dto.request.PartnerReqRequest;
import com.campushub.dto.request.ReviewSubmitRequest;
import com.campushub.entity.*;
import com.campushub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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

    private PartnerService partnerService;

    @BeforeEach
    void setUp() {
        partnerService = new PartnerService(partnerReqRepository, partnerMatchRepository,
                partnerReviewRepository, userCertRepository, userRepository, notificationService);
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
    void createRequest_shouldSucceedWithCertifiedUser() {
        UserCert cert = new UserCert();
        cert.setCertStatus("CERTIFIED");
        when(userCertRepository.findByUserId(anyLong())).thenReturn(Optional.of(cert));

        PartnerReqRequest req = new PartnerReqRequest();
        req.setType("study");
        req.setDescription("找学习的搭子一起备考");
        req.setValidDays(5);
        req.setMaxMembers(3);

        when(partnerReqRepository.save(any(PartnerReq.class))).thenAnswer(inv -> {
            PartnerReq r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        Object result = partnerService.createRequest(1L, req);
        assertNotNull(result);
        verify(partnerReqRepository).save(any(PartnerReq.class));
    }

    @Test
    void applyMatch_shouldRejectSelfApply() {
        UserCert cert = new UserCert();
        cert.setCertStatus("CERTIFIED");
        when(userCertRepository.findByUserId(anyLong())).thenReturn(Optional.of(cert));

        PartnerReq req = new PartnerReq();
        req.setId(1L);
        req.setUserId(1L);
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
        req.setExpireTime(LocalDateTime.now().minusDays(1));
        when(partnerReqRepository.findById(1L)).thenReturn(Optional.of(req));

        assertThrows(BusinessException.class, () -> partnerService.applyMatch(1L, 1L, ""));
    }

    @Test
    void submitReview_shouldRejectDuplicateReview() {
        PartnerMatch match = new PartnerMatch();
        match.setId(1L);
        match.setRequestId(1L);
        match.setApplicantId(2L);
        when(partnerMatchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(partnerReviewRepository.existsByMatchIdAndReviewerId(1L, 1L)).thenReturn(true);

        ReviewSubmitRequest req = new ReviewSubmitRequest();
        req.setRating(5);
        req.setContent("非常靠谱的搭子，好评！");

        assertThrows(BusinessException.class, () -> partnerService.submitReview(1L, 1L, req));
    }
}
