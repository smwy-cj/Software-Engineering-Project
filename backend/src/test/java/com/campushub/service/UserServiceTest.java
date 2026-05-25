package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.dto.request.LoginRequest;
import com.campushub.dto.request.RegisterRequest;
import com.campushub.entity.User;
import com.campushub.repository.AdminRepository;
import com.campushub.repository.UserCertRepository;
import com.campushub.repository.UserRepository;
import com.campushub.security.JwtTokenProvider;
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
    void register_shouldSucceedWithValidData() {
        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800138001");
        req.setSmsCode("123456");
        req.setUsername("测试用户");
        req.setPassword("Test1234");
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
        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800138001");
        req.setAgreeTerms(true);

        when(userRepository.existsByPhone(anyString())).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.register(req));
    }

    @Test
    void register_shouldRejectWrongSmsCode() {
        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800138001");
        req.setSmsCode("000000");
        req.setAgreeTerms(true);

        when(userRepository.existsByPhone(anyString())).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.register(req));
    }

    @Test
    void login_shouldSucceedWithCorrectPassword() {
        LoginRequest req = new LoginRequest();
        req.setPhone("13800138001");
        req.setLoginType("password");
        req.setPassword("Test1234");

        User user = new User();
        user.setId(1L);
        user.setPhone("13800138001");
        user.setUsername("测试用户");
        user.setPasswordHash("hashed");
        user.setAccountStatus("NORMAL");

        when(userRepository.findByPhone(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString())).thenReturn("test-token");

        Object result = userService.login(req);
        assertNotNull(result);
    }

    @Test
    void login_shouldRejectWrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setPhone("13800138001");
        req.setLoginType("password");
        req.setPassword("WrongPass");

        User user = new User();
        user.setId(1L);
        user.setPasswordHash("hashed");
        user.setAccountStatus("NORMAL");
        user.setLoginFailCnt(0);

        when(userRepository.findByPhone(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.login(req));
        assertEquals(1, user.getLoginFailCnt());
    }

    @Test
    void login_shouldRejectBannedAccount() {
        LoginRequest req = new LoginRequest();
        req.setPhone("13800138001");
        req.setLoginType("password");

        User user = new User();
        user.setAccountStatus("BANNED");

        when(userRepository.findByPhone(anyString())).thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(req));
        assertEquals(40103, ex.getCode());
    }
}
