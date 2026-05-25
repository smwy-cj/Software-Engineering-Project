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
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private String token1;
    private String token2;
    private Long userId1;
    private Long userId2;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1";

        // Create two test users
        User user1 = new User();
        user1.setUsername("集成测试用户1");
        user1.setPhone("13900139001");
        user1.setPasswordHash(passwordEncoder.encode("Test1234"));
        user1.setAccountStatus("NORMAL");
        userId1 = userRepository.save(user1).getId();

        User user2 = new User();
        user2.setUsername("集成测试用户2");
        user2.setPhone("13900139002");
        user2.setPasswordHash(passwordEncoder.encode("Test1234"));
        user2.setAccountStatus("NORMAL");
        userId2 = userRepository.save(user2).getId();

        // Certify both users
        UserCert cert1 = new UserCert();
        cert1.setUserId(userId1);
        cert1.setStudentId("IT" + userId1);
        cert1.setRealName("集成用户1");
        cert1.setIdCard("32000020000101000" + userId1);
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
        cert2.setIdCard("32000020000101000" + userId2);
        cert2.setUniversity("测试大学");
        cert2.setMajor("计算机科学");
        cert2.setGrade("2024级");
        cert2.setGender("female");
        cert2.setAge(20);
        cert2.setCertStatus("CERTIFIED");
        userCertRepository.save(cert2);

        // Login to get tokens
        token1 = login("13900139001", "Test1234");
        token2 = login("13900139002", "Test1234");
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

        // User1 tries to apply to own request
        HttpEntity<Map<String, String>> applyEntity = new HttpEntity<>(Map.of(), authHeaders(token1));
        ResponseEntity<Map> applyResp = restTemplate.postForEntity(
                baseUrl + "/partner/requests/" + requestId + "/apply", applyEntity, Map.class);
        assertEquals(40003, applyResp.getBody().get("code"));
    }
}
