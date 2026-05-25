package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.dto.request.*;
import com.campushub.entity.*;
import com.campushub.repository.*;
import com.campushub.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserCertRepository userCertRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(UserRepository userRepository, UserCertRepository userCertRepository,
                       AdminRepository adminRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.userCertRepository = userCertRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public Object register(RegisterRequest req) {
        if (!req.isAgreeTerms()) {
            throw new BusinessException(40003, "请先同意用户协议和隐私政策");
        }
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new BusinessException(40901, "该手机号已注册");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BusinessException(40901, "该用户名已被占用");
        }
        // Mock SMS verification
        if (!"123456".equals(req.getSmsCode())) {
            throw new BusinessException(40003, "短信验证码错误或已过期");
        }

        User user = new User();
        user.setPhone(req.getPhone());
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setAccountStatus("NORMAL");
        user = userRepository.save(user);

        var resp = new java.util.LinkedHashMap<String, Object>();
        resp.put("userId", user.getId());
        resp.put("username", user.getUsername());
        resp.put("phone", maskPhone(user.getPhone()));
        resp.put("createdAt", user.getCreatedAt());
        return resp;
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByPhone(req.getPhone())
                .orElseThrow(() -> new BusinessException(40101, "手机号或密码错误"));

        if ("BANNED".equals(user.getAccountStatus())) {
            throw new BusinessException(40103, "账号已被封禁");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(40103, "账号已因多次登录失败被锁定，请15分钟后重试");
        }

        if ("password".equals(req.getLoginType())) {
            if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
                user.setLoginFailCnt(user.getLoginFailCnt() + 1);
                if (user.getLoginFailCnt() >= 5) {
                    user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                }
                userRepository.save(user);
                int remaining = 5 - user.getLoginFailCnt();
                throw new BusinessException(40101,
                        "手机号或密码错误，还剩 " + Math.max(0, remaining) + " 次尝试机会");
            }
        } else if ("smsCode".equals(req.getLoginType())) {
            if (!"123456".equals(req.getSmsCode())) {
                throw new BusinessException(40101, "短信验证码错误");
            }
        }

        // Login success - reset fail count
        user.setLoginFailCnt(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        UserCert cert = userCertRepository.findByUserId(user.getId()).orElse(null);
        Admin admin = adminRepository.findByUserId(user.getId()).orElse(null);

        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        LoginResponse resp = new LoginResponse();
        resp.setAccessToken(token);
        resp.setExpiresIn(7200);

        LoginResponse.UserInfo info = new LoginResponse.UserInfo();
        info.setUserId(user.getId());
        info.setUsername(user.getUsername());
        info.setAvatar(user.getAvatar());
        info.setCertStatus(cert != null ? cert.getCertStatus() : "UNCERTIFIED");
        info.setAccountStatus(user.getAccountStatus());
        resp.setUserInfo(info);

        return resp;
    }

    public Object sendSmsCode(SmsCodeRequest req) {
        var resp = new java.util.LinkedHashMap<String, Object>();
        resp.put("expiresIn", 300);
        resp.put("retryAfter", 60);
        return resp;
    }

    public Object getCaptcha() {
        var resp = new java.util.LinkedHashMap<String, Object>();
        resp.put("captchaId", "captcha-" + System.currentTimeMillis());
        resp.put("captchaImage", "data:image/png;base64,mock-captcha-image");
        return resp;
    }

    @Transactional
    public Object certify(Long userId, UserCert certData) {
        UserCert cert = new UserCert();
        cert.setUserId(userId);
        cert.setStudentId(certData.getStudentId());
        cert.setRealName(certData.getRealName());
        cert.setIdCard(certData.getIdCard());
        cert.setUniversity(certData.getUniversity());
        cert.setMajor(certData.getMajor());
        cert.setGrade(certData.getGrade());
        cert.setGender(certData.getGender());
        cert.setAge(certData.getAge());
        cert.setInterests(certData.getInterests());
        cert.setCertStatus("CERTIFIED");
        cert = userCertRepository.save(cert);

        var resp = new java.util.LinkedHashMap<String, Object>();
        resp.put("certId", cert.getId());
        resp.put("certStatus", cert.getCertStatus());
        return resp;
    }

    public Object getCertStatus(Long userId) {
        UserCert cert = userCertRepository.findByUserId(userId).orElse(null);
        var resp = new java.util.LinkedHashMap<String, Object>();
        if (cert != null) {
            resp.put("certStatus", cert.getCertStatus());
            resp.put("university", cert.getUniversity());
            resp.put("major", cert.getMajor());
            resp.put("grade", cert.getGrade());
            resp.put("gender", cert.getGender());
            resp.put("age", cert.getAge());
        } else {
            resp.put("certStatus", "UNCERTIFIED");
        }
        return resp;
    }

    private String maskPhone(String phone) {
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        return phone;
    }
}
