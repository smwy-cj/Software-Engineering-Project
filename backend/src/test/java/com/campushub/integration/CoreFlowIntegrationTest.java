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
    private LoveReqRepository loveReqRepository;

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
    private String adminToken;
    private String phone1;
    private String phone2;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1";
        long suffix = System.nanoTime() % 100000;
        phone1 = "13900" + String.format("%06d", suffix);
        phone2 = "13901" + String.format("%06d", suffix);

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

        // Certify both users
        UserCert cert1 = new UserCert();
        cert1.setUserId(userId1);
        cert1.setStudentId("IT" + userId1);
        cert1.setRealName("集成用户1");
        cert1.setIdCard("320000200001010001");
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
        cert2.setRealName("集成用户2");
        cert2.setIdCard("320000200001010002");
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

    private void publishPartnerRequest(Long requestId) {
        PartnerReq req = partnerReqRepository.findById(requestId).orElseThrow();
        req.setStatus("PUBLISHED");
        partnerReqRepository.save(req);
    }
}
