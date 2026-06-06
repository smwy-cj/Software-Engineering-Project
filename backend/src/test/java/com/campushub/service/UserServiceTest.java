package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.dto.request.CertifyRequest;
import com.campushub.dto.request.LoginRequest;
import com.campushub.dto.request.RegisterRequest;
import com.campushub.dto.request.UserProfileRequest;
import com.campushub.entity.User;
import com.campushub.entity.UserCert;
import com.campushub.repository.AdminRepository;
import com.campushub.repository.UserCertRepository;
import com.campushub.repository.UserRepository;
import com.campushub.security.JwtTokenProvider;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserCertRepository userCertRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userCertRepository, adminRepository,
                passwordEncoder, jwtTokenProvider);
    }

    @Test
    void register_shouldSucceedWithValidCaptchaAndMatchingStrongPasswords() {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> captcha = (java.util.Map<String, Object>) userService.getCaptcha();

        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800138001");
        req.setUsername("测试用户");
        req.setPassword("Test1234");
        req.setConfirmPassword("Test1234");
        req.setCaptchaId((String) captcha.get("captchaId"));
        req.setCaptchaCode((String) captcha.get("captchaCode"));
        req.setAgreeTerms(true);

        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        Object result = userService.register(req);
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldRejectWithoutAgreeTerms() {
        RegisterRequest req = new RegisterRequest();
        req.setAgreeTerms(false);

        assertThrows(BusinessException.class, () -> userService.register(req));
    }

    @Test
    void register_shouldRejectDuplicatePhone() {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> captcha = (java.util.Map<String, Object>) userService.getCaptcha();
        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800138001");
        req.setUsername("测试用户");
        req.setPassword("Test1234");
        req.setConfirmPassword("Test1234");
        req.setCaptchaId((String) captcha.get("captchaId"));
        req.setCaptchaCode((String) captcha.get("captchaCode"));
        req.setAgreeTerms(true);

        when(userRepository.existsByPhone(anyString())).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.register(req));
    }

    @Test
    void register_shouldRejectWrongCaptcha() {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> captcha = (java.util.Map<String, Object>) userService.getCaptcha();
        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800138001");
        req.setUsername("测试用户");
        req.setPassword("Test1234");
        req.setConfirmPassword("Test1234");
        req.setCaptchaId((String) captcha.get("captchaId"));
        req.setCaptchaCode("zzzz");
        req.setAgreeTerms(true);

        assertThrows(BusinessException.class, () -> userService.register(req));
    }

    @Test
    void register_shouldRejectInvalidPhoneFormat() {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> captcha = (java.util.Map<String, Object>) userService.getCaptcha();
        RegisterRequest req = new RegisterRequest();
        req.setPhone("12345");
        req.setUsername("测试用户");
        req.setPassword("Test1234");
        req.setConfirmPassword("Test1234");
        req.setCaptchaId((String) captcha.get("captchaId"));
        req.setCaptchaCode((String) captcha.get("captchaCode"));
        req.setAgreeTerms(true);

        assertThrows(BusinessException.class, () -> userService.register(req));
    }

    @Test
    void register_shouldRejectMismatchedConfirmPassword() {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> captcha = (java.util.Map<String, Object>) userService.getCaptcha();
        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800138001");
        req.setUsername("测试用户");
        req.setPassword("Test1234");
        req.setConfirmPassword("Test5678");
        req.setCaptchaId((String) captcha.get("captchaId"));
        req.setCaptchaCode((String) captcha.get("captchaCode"));
        req.setAgreeTerms(true);

        assertThrows(BusinessException.class, () -> userService.register(req));
    }

    @Test
    void register_shouldRejectPasswordWithoutLettersOrDigits() {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> captcha = (java.util.Map<String, Object>) userService.getCaptcha();
        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800138001");
        req.setUsername("测试用户");
        req.setPassword("12345678");
        req.setConfirmPassword("12345678");
        req.setCaptchaId((String) captcha.get("captchaId"));
        req.setCaptchaCode((String) captcha.get("captchaCode"));
        req.setAgreeTerms(true);

        assertThrows(BusinessException.class, () -> userService.register(req));
    }

    @Test
    void registerRequest_shouldAllowAnyUsernameUpToSixteenCharacters() {
        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800138001");
        req.setUsername("u_1");
        req.setPassword("Test1234");
        req.setConfirmPassword("Test1234");
        req.setCaptchaId("captcha-id");
        req.setCaptchaCode("A1b2");
        req.setAgreeTerms(true);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(req);

            assertTrue(violations.stream()
                    .noneMatch(v -> "username".equals(v.getPropertyPath().toString())));
        }
    }

    @Test
    void register_shouldRejectUsernameLongerThanSixteenCharacters() {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> captcha = (java.util.Map<String, Object>) userService.getCaptcha();
        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800138001");
        req.setUsername("abcdefghijklmnopq");
        req.setPassword("Test1234");
        req.setConfirmPassword("Test1234");
        req.setCaptchaId((String) captcha.get("captchaId"));
        req.setCaptchaCode((String) captcha.get("captchaCode"));
        req.setAgreeTerms(true);

        assertThrows(BusinessException.class, () -> userService.register(req));
    }

    @Test
    void getCaptcha_shouldReturnFourAlphanumericCharacters() {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> captcha = (java.util.Map<String, Object>) userService.getCaptcha();

        assertNotNull(captcha.get("captchaId"));
        assertTrue(((String) captcha.get("captchaCode")).matches("^[A-Za-z0-9]{4}$"));
    }

    @Test
    void updateProfile_shouldUpdateAccountAndPersonalFieldsForCertifiedUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("oldName");
        user.setAvatar("old-avatar");

        UserCert cert = new UserCert();
        cert.setUserId(1L);
        cert.setCertStatus("CERTIFIED");
        cert.setGender("male");
        cert.setAge(20);
        cert.setGrade("2022级");
        cert.setMajor("计算机科学与技术");

        UserProfileRequest req = new UserProfileRequest();
        req.setNickname("newName");
        req.setAvatar("https://example.com/avatar.png");
        req.setMajor("软件工程");
        req.setGrade("2023级");
        req.setBio("保持好奇，认真生活");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userCertRepository.findByUserId(1L)).thenReturn(Optional.of(cert));
        when(userRepository.existsByUsername("newName")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userCertRepository.save(any(UserCert.class))).thenAnswer(inv -> inv.getArgument(0));

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> result = (java.util.Map<String, Object>) userService.updateProfile(1L, req);

        assertEquals("newName", result.get("username"));
        assertEquals("newName", result.get("nickname"));
        assertEquals("https://example.com/avatar.png", result.get("avatar"));
        assertEquals("2023级", result.get("grade"));
        assertEquals("软件工程", result.get("major"));
        assertEquals("保持好奇，认真生活", result.get("bio"));
        assertEquals("male", cert.getGender());
        assertEquals(20, cert.getAge());
    }

    @Test
    void updateProfile_shouldRejectDuplicateUsername() {
        User user = new User();
        user.setId(1L);
        user.setUsername("oldName");

        UserCert cert = new UserCert();
        cert.setUserId(1L);
        cert.setCertStatus("CERTIFIED");

        UserProfileRequest req = new UserProfileRequest();
        req.setNickname("takenName");
        req.setGrade("2023级");
        req.setMajor("软件工程");
        req.setBio("保持好奇");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userCertRepository.findByUserId(1L)).thenReturn(Optional.of(cert));
        when(userRepository.existsByUsername("takenName")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.updateProfile(1L, req));
        assertEquals(40901, ex.getCode());
    }

    @Test
    void certify_shouldOnlyRequireBasicCampusIdentityFields() {
        CertifyRequest req = new CertifyRequest();
        req.setStudentId("20240001");
        req.setSchool("青隅大学");
        req.setGender("male");
        req.setAge(20);

        when(userCertRepository.save(any(UserCert.class))).thenAnswer(inv -> {
            UserCert cert = inv.getArgument(0);
            cert.setId(10L);
            return cert;
        });

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> result = (java.util.Map<String, Object>) userService.certify(1L, req);

        verify(userCertRepository).save(argThat(cert ->
                "20240001".equals(cert.getStudentId())
                        && "青隅大学".equals(cert.getUniversity())
                        && "male".equals(cert.getGender())
                        && cert.getAge() == 20
                        && cert.getRealName() == null
                        && cert.getIdCard() == null
                        && cert.getMajor() == null
                        && cert.getGrade() == null
        ));
        assertEquals("CERTIFIED", result.get("verificationStatus"));
        assertFalse(result.containsKey("realName"));
        assertFalse(result.containsKey("idCard"));
    }

    @Test
    void getCertStatus_shouldReturnVerificationInfoWithoutRealNameOrIdCard() {
        User user = new User();
        user.setId(1L);
        user.setUsername("测试用户");

        UserCert cert = new UserCert();
        cert.setUserId(1L);
        cert.setStudentId("20240001");
        cert.setUniversity("青隅大学");
        cert.setGender("female");
        cert.setAge(21);
        cert.setCertStatus("CERTIFIED");
        cert.setRealName("不应返回");
        cert.setIdCard("320000200001010001");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userCertRepository.findByUserId(1L)).thenReturn(Optional.of(cert));

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> result = (java.util.Map<String, Object>) userService.getCertStatus(1L);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> verificationInfo =
                (java.util.Map<String, Object>) result.get("verificationInfo");

        assertEquals("20240001", verificationInfo.get("studentId"));
        assertEquals("青隅大学", verificationInfo.get("school"));
        assertEquals("female", verificationInfo.get("gender"));
        assertEquals(21, verificationInfo.get("age"));
        assertEquals("CERTIFIED", verificationInfo.get("verificationStatus"));
        assertFalse(verificationInfo.containsKey("realName"));
        assertFalse(verificationInfo.containsKey("idCard"));
    }

    @Test
    void login_shouldSucceedWithCorrectPassword() {
        LoginRequest req = new LoginRequest();
        req.setPhone("13800138001");
        req.setLoginType("password");
        req.setPassword("Test1234");
        req.setCaptcha("mock");
        req.setCaptchaId("mock");

        User user = new User();
        user.setId(1L);
        user.setPhone("13800138001");
        user.setUsername("测试用户");
        user.setPasswordHash("hashed");
        user.setAccountStatus("NORMAL");

        when(userRepository.findByPhone(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString(), anyBoolean())).thenReturn("test-token");

        Object result = userService.login(req);
        assertNotNull(result);
    }

    @Test
    void login_shouldRejectWrongPasswordAndLockAfterThreeFailures() {
        LoginRequest req = new LoginRequest();
        req.setPhone("13800138001");
        req.setLoginType("password");
        req.setPassword("WrongPass");
        req.setCaptcha("mock");
        req.setCaptchaId("mock");

        User user = new User();
        user.setId(1L);
        user.setPasswordHash("hashed");
        user.setAccountStatus("NORMAL");
        user.setLoginFailCnt(0);

        when(userRepository.findByPhone(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.login(req));
        assertEquals(1, user.getLoginFailCnt());
        user.setLoginFailCnt(2);
        assertThrows(BusinessException.class, () -> userService.login(req));
        assertNotNull(user.getLockedUntil());
    }

    @Test
    void login_shouldRejectBannedAccount() {
        LoginRequest req = new LoginRequest();
        req.setPhone("13800138001");
        req.setLoginType("password");
        req.setCaptcha("mock");
        req.setCaptchaId("mock");

        User user = new User();
        user.setAccountStatus("BANNED");

        when(userRepository.findByPhone(anyString())).thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(req));
        assertEquals(40103, ex.getCode());
    }
}
