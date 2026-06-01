package com.campushub.service;

import com.campushub.entity.LoveMatch;
import com.campushub.entity.LoveReq;
import com.campushub.entity.PartnerMatch;
import com.campushub.entity.PartnerReq;
import com.campushub.entity.TreeHolePost;
import com.campushub.repository.LoveMatchRepository;
import com.campushub.repository.LoveReqRepository;
import com.campushub.repository.PartnerMatchRepository;
import com.campushub.repository.PartnerReqRepository;
import com.campushub.repository.TreeHolePostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileActivityServiceTest {
    @Mock private TreeHolePostRepository treeHolePostRepository;
    @Mock private PartnerReqRepository partnerReqRepository;
    @Mock private LoveReqRepository loveReqRepository;
    @Mock private PartnerMatchRepository partnerMatchRepository;
    @Mock private LoveMatchRepository loveMatchRepository;

    private ProfileActivityService service;

    @BeforeEach
    void setUp() {
        service = new ProfileActivityService(treeHolePostRepository, partnerReqRepository, loveReqRepository,
                partnerMatchRepository, loveMatchRepository);
    }

    @Test
    void listPublished_shouldGroupCurrentUserContentByModule() {
        TreeHolePost post = new TreeHolePost();
        post.setId(1L);
        post.setContent("树洞内容");
        post.setCategory("life");
        post.setStatus("PUBLISHED");

        PartnerReq partnerReq = new PartnerReq();
        partnerReq.setId(2L);
        partnerReq.setType("study");
        partnerReq.setDescription("搭子内容");
        partnerReq.setStatus("PENDING");

        LoveReq loveReq = new LoveReq();
        loveReq.setId(3L);
        loveReq.setDescription("交友需求");
        loveReq.setStatus("PUBLISHED");

        when(treeHolePostRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(post));
        when(partnerReqRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(partnerReq));
        when(loveReqRepository.findTop20ByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(loveReq));

        Map<String, Object> result = service.listPublished(1L);

        assertEquals(1, section(result, "treeHole").size());
        assertEquals(1, section(result, "partner").size());
        assertEquals(1, section(result, "love").size());
    }

    @Test
    void listApplications_shouldGroupCurrentUserApplicationRecords() {
        PartnerReq partnerReq = new PartnerReq();
        partnerReq.setId(2L);
        partnerReq.setDescription("搭子目标");

        PartnerMatch partnerMatch = new PartnerMatch();
        partnerMatch.setId(10L);
        partnerMatch.setRequestId(2L);
        partnerMatch.setApplicantId(1L);
        partnerMatch.setStatus("PENDING");
        partnerMatch.setApplyMessage("一起学习");

        LoveReq loveReq = new LoveReq();
        loveReq.setId(3L);
        loveReq.setDescription("交友目标");

        LoveMatch loveMatch = new LoveMatch();
        loveMatch.setId(11L);
        loveMatch.setRequestId(3L);
        loveMatch.setApplicantId(1L);
        loveMatch.setStatus("ACCEPTED");

        when(partnerMatchRepository.findTop20ByApplicantIdOrderByApplyTimeDesc(1L)).thenReturn(List.of(partnerMatch));
        when(partnerReqRepository.findById(2L)).thenReturn(Optional.of(partnerReq));
        when(loveMatchRepository.findTop20ByApplicantIdOrderByApplyTimeDesc(1L)).thenReturn(List.of(loveMatch));
        when(loveReqRepository.findById(3L)).thenReturn(Optional.of(loveReq));

        Map<String, Object> result = service.listApplications(1L);

        assertEquals(1, section(result, "partner").size());
        assertEquals(1, section(result, "love").size());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> section(Map<String, Object> result, String key) {
        return (List<Map<String, Object>>) result.get(key);
    }
}
