package com.campushub.integration;

import com.campushub.common.ApiResponse;
import com.campushub.dto.request.*;
import com.campushub.entity.*;
import com.campushub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CoreFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCertRepository userCertRepository;

    @Autowired
    private PartnerReqRepository partnerReqRepository;

    @Autowired
    private TreeHolePostRepository treeHolePostRepository;

    @Autowired
    private LoveReqRepository loveReqRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReviewRecordRepository reviewRecordRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private String token1;
    private String token2;
    private Long userId1;
    private Long userId2;
    private Long userId3;
    private String adminToken;
    private String phone1;
    private String phone2;
    private String phone3;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1";
        long suffix = System.nanoTime() % 100000;
        phone1 = "13900" + String.format("%06d", suffix);
        phone2 = "13901" + String.format("%06d", suffix);
        phone3 = "13903" + String.format("%06d", suffix);

        // Create two test users
        User user1 = new User();
        user1.setUsername("集成测试用户1" + suffix);
        user1.setPhone(phone1);
        user1.setPasswordHash(passwordEncoder.encode("Test1234"));
        user1.setAccountStatus("NORMAL");
        userId1 = userRepository.save(user1).getId();

        User user2 = new User();
        user2.setUsername("集成测试用户2" + suffix);
        user2.setPhone(phone2);
        user2.setPasswordHash(passwordEncoder.encode("Test1234"));
        user2.setAccountStatus("NORMAL");
        userId2 = userRepository.save(user2).getId();

        User user3 = new User();
        user3.setUsername("新用户C" + suffix);
        user3.setPhone(phone3);
        user3.setPasswordHash(passwordEncoder.encode("Test1234"));
        user3.setAccountStatus("NORMAL");
        userId3 = userRepository.save(user3).getId();

        // Certify both users
        UserCert cert1 = new UserCert();
        cert1.setUserId(userId1);
        cert1.setStudentId("IT" + userId1);
        cert1.setUniversity("测试大学");
        cert1.setMajor("计算机科学");
        cert1.setGrade("2024级");
        cert1.setGender("male");
        cert1.setAge(20);
        cert1.setCertStatus("CERTIFIED");
        userCertRepository.save(cert1);

        UserCert cert2 = new UserCert();
        cert2.setUserId(userId2);
        cert2.setStudentId("IT" + userId2);
        cert2.setUniversity("测试大学");
        cert2.setMajor("计算机科学");
        cert2.setGrade("2024级");
        cert2.setGender("female");
        cert2.setAge(20);
        cert2.setCertStatus("CERTIFIED");
        userCertRepository.save(cert2);

        // Login to get tokens
        token1 = login(phone1, "Test1234");
        token2 = login(phone2, "Test1234");
        adminToken = createAdminAndLogin(suffix);
    }

    private String createAdminAndLogin(long suffix) {
        String adminPhone = "13902" + String.format("%06d", suffix);
        User adminUser = new User();
        adminUser.setUsername("集成管理员" + suffix);
        adminUser.setPhone(adminPhone);
        adminUser.setPasswordHash(passwordEncoder.encode("Admin123"));
        adminUser.setAccountStatus("NORMAL");
        adminUser = userRepository.save(adminUser);

        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setAdminLevel("SUPER");
        admin.setPermissions("[\"review\"]");
        adminRepository.save(admin);

        return login(adminPhone, "Admin123");
    }

    private String login(String phone, String password) {
        LoginRequest req = new LoginRequest();
        req.setPhone(phone);
        req.setLoginType("password");
        req.setPassword(password);
        req.setCaptcha("mock");
        req.setCaptchaId("mock");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<Map> resp = restTemplate.postForEntity(baseUrl + "/auth/login", entity, Map.class);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        return (String) data.get("accessToken");
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void testCompleteNormalFlow() {
        // Step 1: User2 creates a partner request
        PartnerReqRequest req = new PartnerReqRequest();
        req.setType("study");
        req.setDescription("寻找一起备考六级的学习搭子，每天下午图书馆自习");
        req.setConditions("{\"grade\":\"2024级\"}");
        req.setValidDays(5);
        req.setMaxMembers(3);

        HttpEntity<PartnerReqRequest> reqEntity = new HttpEntity<>(req, authHeaders(token2));
        ResponseEntity<Map> createResp = restTemplate.postForEntity(
                baseUrl + "/partner/requests", reqEntity, Map.class);
        assertEquals(200, createResp.getBody().get("code"));

        Map<String, Object> data = (Map<String, Object>) createResp.getBody().get("data");
        Long requestId = ((Number) data.get("requestId")).longValue();
        publishPartnerRequest(requestId);

        // Step 2: User1 applies for the request
        Map<String, String> applyBody = Map.of("message", "一起加油！");
        HttpEntity<Map<String, String>> applyEntity = new HttpEntity<>(applyBody, authHeaders(token1));
        ResponseEntity<Map> applyResp = restTemplate.postForEntity(
                baseUrl + "/partner/requests/" + requestId + "/apply", applyEntity, Map.class);
        assertEquals(200, applyResp.getBody().get("code"));

        Map<String, Object> matchData = (Map<String, Object>) applyResp.getBody().get("data");
        Long matchId = ((Number) matchData.get("matchId")).longValue();

        // Step 3: Verify match detail is accessible
        HttpEntity<Void> detailEntity = new HttpEntity<>(authHeaders(token1));
        ResponseEntity<Map> detailResp = restTemplate.exchange(
                baseUrl + "/partner/matches/" + matchId, HttpMethod.GET, detailEntity, Map.class);
        assertEquals(200, detailResp.getBody().get("code"));

        // Step 4: Submit review (manual acceptance first)
        PartnerMatch match = new PartnerMatch();
        // ... would need to accept the match first in real flow

        assertNotNull(matchId);
    }

    @Test
    void testExceptionFlow_UnauthorizedAccess() {
        // Test accessing protected endpoint without token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> resp = restTemplate.exchange(
                baseUrl + "/partner/requests", HttpMethod.GET, entity, Map.class);
        assertEquals(403, resp.getStatusCode().value());
    }

    @Test
    void testExceptionFlow_SelfApply() {
        // Create a request by user1
        PartnerReqRequest req = new PartnerReqRequest();
        req.setType("study");
        req.setDescription("测试自我申请拒绝的场景描述");
        req.setConditions("{}");
        req.setValidDays(5);
        req.setMaxMembers(3);

        HttpEntity<PartnerReqRequest> reqEntity = new HttpEntity<>(req, authHeaders(token1));
        ResponseEntity<Map> createResp = restTemplate.postForEntity(
                baseUrl + "/partner/requests", reqEntity, Map.class);
        Map<String, Object> data = (Map<String, Object>) createResp.getBody().get("data");
        Long requestId = ((Number) data.get("requestId")).longValue();
        publishPartnerRequest(requestId);

        // User1 tries to apply to own request
        HttpEntity<Map<String, String>> applyEntity = new HttpEntity<>(Map.of(), authHeaders(token1));
        ResponseEntity<Map> applyResp = restTemplate.postForEntity(
                baseUrl + "/partner/requests/" + requestId + "/apply", applyEntity, Map.class);
        assertEquals(40003, applyResp.getBody().get("code"));
    }

    @Test
    void testMultiUserTreeHoleInteractionCreatesOwnerNotificationsAndIsolatedProfileData() {
        Map<String, Object> createBody = Map.of(
                "content", "今天在图书馆复习数据库课程，想找同学一起交流复习节奏。",
                "category", "study",
                "allowComment", true,
                "allowLike", true);
        ResponseEntity<Map> createResp = restTemplate.postForEntity(
                baseUrl + "/treehole/posts", new HttpEntity<>(createBody, authHeaders(token1)), Map.class);
        assertEquals(200, createResp.getBody().get("code"));
        Long postId = ((Number) ((Map<String, Object>) createResp.getBody().get("data")).get("postId")).longValue();
        approveReview("treeholePost", postId, "PASSED");

        ResponseEntity<Map> likeResp = restTemplate.postForEntity(
                baseUrl + "/treehole/posts/" + postId + "/like",
                new HttpEntity<>(Map.of(), authHeaders(token2)), Map.class);
        assertEquals(200, likeResp.getBody().get("code"));
        assertEquals(1, ((Number) ((Map<String, Object>) likeResp.getBody().get("data")).get("likeCount")).intValue());

        ResponseEntity<Map> commentResp = restTemplate.postForEntity(
                baseUrl + "/treehole/posts/" + postId + "/comments",
                new HttpEntity<>(Map.of("content", "我也在复习这门课，可以一起整理重点。"), authHeaders(token2)), Map.class);
        assertEquals(200, commentResp.getBody().get("code"));

        TreeHolePost post = treeHolePostRepository.findById(postId).orElseThrow();
        assertEquals(1, post.getLikeCount());
        assertEquals(1, post.getCommentCount());

        Map<String, Object> ownerNotifications = getNotifications(token1);
        assertEquals(3, ((Number) ownerNotifications.get("unreadCount")).intValue());
        assertTrue(((java.util.List<Map<String, Object>>) ownerNotifications.get("content")).stream()
                .anyMatch(n -> "treehole_like".equals(n.get("type"))));
        assertTrue(((java.util.List<Map<String, Object>>) ownerNotifications.get("content")).stream()
                .anyMatch(n -> "treehole_comment".equals(n.get("type"))));

        Map<String, Object> user2Notifications = getNotifications(token2);
        assertEquals(0, ((Number) user2Notifications.get("unreadCount")).intValue());

        Map<String, Object> user1Published = getData("/profile/published", token1);
        assertEquals(1, ((java.util.List<?>) user1Published.get("treeHole")).size());
        Map<String, Object> user2Published = getData("/profile/published", token2);
        assertTrue(((java.util.List<?>) user2Published.get("treeHole")).isEmpty());
    }

    @Test
    void testPartnerApplicationStatusAndNotificationUseUserFacingChineseText() {
        PartnerReqRequest req = new PartnerReqRequest();
        req.setType("study");
        req.setDescription("寻找一位软件工程课程结对复盘搭子，每周两次同步进度");
        req.setConditions("{}");
        req.setValidDays(5);
        req.setMaxMembers(1);

        ResponseEntity<Map> createResp = restTemplate.postForEntity(
                baseUrl + "/partner/requests", new HttpEntity<>(req, authHeaders(token1)), Map.class);
        assertEquals(200, createResp.getBody().get("code"));
        Long requestId = ((Number) ((Map<String, Object>) createResp.getBody().get("data")).get("requestId")).longValue();

        ResponseEntity<Map> applyResp = restTemplate.postForEntity(
                baseUrl + "/partner/requests/" + requestId + "/apply",
                new HttpEntity<>(Map.of("message", "我可以固定参加"), authHeaders(token2)), Map.class);
        assertEquals(200, applyResp.getBody().get("code"));
        Long matchId = ((Number) ((Map<String, Object>) applyResp.getBody().get("data")).get("matchId")).longValue();

        ResponseEntity<Map> acceptResp = restTemplate.exchange(
                baseUrl + "/partner/matches/" + matchId,
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("status", "ACCEPTED"), authHeaders(token1)),
                Map.class);
        assertEquals(200, acceptResp.getBody().get("code"));

        Map<String, Object> applications = getData("/profile/applications", token2);
        Map<String, Object> partnerApplication = ((java.util.List<Map<String, Object>>) applications.get("partner")).get(0);
        assertEquals("已接受", partnerApplication.get("statusText"));

        Map<String, Object> applicantNotifications = getNotifications(token2);
        assertTrue(((java.util.List<Map<String, Object>>) applicantNotifications.get("content")).stream()
                .anyMatch(n -> String.valueOf(n.get("content")).contains("已接受")));
        assertFalse(((java.util.List<Map<String, Object>>) applicantNotifications.get("content")).stream()
                .anyMatch(n -> String.valueOf(n.get("content")).contains("ACCEPTED")));

        ResponseEntity<Map> duplicateResp = restTemplate.postForEntity(
                baseUrl + "/partner/requests/" + requestId + "/apply",
                new HttpEntity<>(Map.of(), authHeaders(token2)), Map.class);
        assertEquals(40003, duplicateResp.getBody().get("code"));
    }

    @Test
    void testUncertifiedUserAndAdminPermissionBoundaries() {
        String token3 = login(phone3, "Test1234");

        PartnerReqRequest req = new PartnerReqRequest();
        req.setType("study");
        req.setDescription("未认证用户不应能发布搭子需求");
        req.setConditions("{}");
        req.setValidDays(3);
        req.setMaxMembers(2);
        ResponseEntity<Map> uncertifiedPartnerResp = restTemplate.postForEntity(
                baseUrl + "/partner/requests", new HttpEntity<>(req, authHeaders(token3)), Map.class);
        assertEquals(40302, uncertifiedPartnerResp.getBody().get("code"));

        ResponseEntity<Map> normalUserAdminResp = restTemplate.exchange(
                baseUrl + "/admin/reviews/pending", HttpMethod.GET,
                new HttpEntity<>(authHeaders(token1)), Map.class);
        assertEquals(403, normalUserAdminResp.getStatusCode().value());

        ResponseEntity<Map> anonymousCertResp = restTemplate.postForEntity(
                baseUrl + "/auth/certify",
                new HttpEntity<>(Map.of("studentId", "20260003", "school", "测试大学", "gender", "男", "age", 19)),
                Map.class);
        assertEquals(403, anonymousCertResp.getStatusCode().value());
    }

    @Test
    void testLoveRequestVisibleAfterAdminApproval() {
        Map<String, Object> body = Map.of(
                "description", "希望认识愿意一起自习和散步的同校同学",
                "validDays", 7,
                "scope", "sameSchool");
        HttpEntity<Map<String, Object>> createEntity = new HttpEntity<>(body, authHeaders(token1));

        ResponseEntity<Map> createResp = restTemplate.postForEntity(
                baseUrl + "/love/requests", createEntity, Map.class);
        assertEquals(200, createResp.getBody().get("code"));
        Map<String, Object> createData = (Map<String, Object>) createResp.getBody().get("data");
        Long requestId = ((Number) createData.get("requestId")).longValue();

        assertTrue(listLoveRequestsForUser2().stream()
                .noneMatch(item -> requestId.equals(((Number) item.get("requestId")).longValue())));

        ReviewRecord review = reviewRecordRepository.findAll().stream()
                .filter(r -> "loveReq".equals(r.getContentType()) && requestId.equals(r.getContentId()))
                .findFirst()
                .orElseThrow();
        HttpEntity<Map<String, String>> reviewEntity = new HttpEntity<>(
                Map.of("result", "PASSED", "comment", "审核通过"), authHeaders(adminToken));
        ResponseEntity<Map> reviewResp = restTemplate.exchange(
                baseUrl + "/admin/reviews/" + review.getId(), HttpMethod.PUT, reviewEntity, Map.class);
        assertEquals(200, reviewResp.getBody().get("code"));
        assertEquals("PUBLISHED", loveReqRepository.findById(requestId).orElseThrow().getStatus());

        assertTrue(listLoveRequestsForUser2().stream()
                .anyMatch(item -> requestId.equals(((Number) item.get("requestId")).longValue())));
    }

    private java.util.List<Map<String, Object>> listLoveRequestsForUser2() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(token2));
        ResponseEntity<Map> resp = restTemplate.exchange(
                baseUrl + "/love/requests?sortBy=published&size=50", HttpMethod.GET, entity, Map.class);
        assertEquals(200, resp.getBody().get("code"));
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        return (java.util.List<Map<String, Object>>) data.get("content");
    }

    private Map<String, Object> getNotifications(String token) {
        return getData("/notifications?size=50", token);
    }

    private Map<String, Object> getData(String path, String token) {
        ResponseEntity<Map> resp = restTemplate.exchange(
                baseUrl + path, HttpMethod.GET, new HttpEntity<>(authHeaders(token)), Map.class);
        assertEquals(200, resp.getBody().get("code"));
        return (Map<String, Object>) resp.getBody().get("data");
    }

    private void approveReview(String contentType, Long contentId, String result) {
        ReviewRecord review = reviewRecordRepository.findAll().stream()
                .filter(r -> contentType.equals(r.getContentType()) && contentId.equals(r.getContentId()))
                .findFirst()
                .orElseThrow();
        HttpEntity<Map<String, String>> reviewEntity = new HttpEntity<>(
                Map.of("result", result, "comment", "测试审核"), authHeaders(adminToken));
        ResponseEntity<Map> reviewResp = restTemplate.exchange(
                baseUrl + "/admin/reviews/" + review.getId(), HttpMethod.PUT, reviewEntity, Map.class);
        assertEquals(200, reviewResp.getBody().get("code"));
    }

    private void publishPartnerRequest(Long requestId) {
        PartnerReq req = partnerReqRepository.findById(requestId).orElseThrow();
        req.setStatus("PUBLISHED");
        partnerReqRepository.save(req);
    }
}
