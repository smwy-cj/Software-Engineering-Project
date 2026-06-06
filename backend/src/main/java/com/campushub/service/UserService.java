package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.dto.request.*;
import com.campushub.entity.*;
import com.campushub.repository.*;
import com.campushub.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserCertRepository userCertRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final Map<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    private static final String CAPTCHA_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final long CAPTCHA_TTL_SECONDS = 300;

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
        validateRegisterInput(req);
        verifyCaptcha(req.getCaptchaId(), req.getCaptchaCode());
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new BusinessException(40901, "该手机号已注册");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BusinessException(40901, "该用户名已被占用");
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
        if (!StringUtils.hasText(req.getCaptcha()) || !StringUtils.hasText(req.getCaptchaId())) {
            throw new BusinessException(40003, "请先完成图形验证码验证");
        }
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
                if (user.getLoginFailCnt() >= 3) {
                    user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                }
                userRepository.save(user);
                int remaining = 3 - user.getLoginFailCnt();
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

        String role = admin != null ? "ADMIN" : "USER";
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), role, req.isRememberMe());
        LoginResponse resp = new LoginResponse();
        resp.setAccessToken(token);
        resp.setExpiresIn(req.isRememberMe() ? 604800 : 7200);

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
        String captchaId = "captcha-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
        String captchaCode = generateCaptchaCode();
        captchaStore.put(captchaId, new CaptchaEntry(captchaCode, LocalDateTime.now().plusSeconds(CAPTCHA_TTL_SECONDS)));
        resp.put("captchaId", captchaId);
        resp.put("captchaCode", captchaCode);
        resp.put("expiresIn", CAPTCHA_TTL_SECONDS);
        return resp;
    }

    @Transactional
    public Object certify(Long userId, CertifyRequest req) {
        UserCert cert = new UserCert();
        cert.setUserId(userId);
        cert.setStudentId(req.getStudentId());
        cert.setUniversity(req.getSchool());
        cert.setGender(req.getGender());
        cert.setAge(req.getAge());
        cert.setCertStatus("CERTIFIED");
        cert = userCertRepository.save(cert);

        var resp = new java.util.LinkedHashMap<String, Object>();
        resp.put("certId", cert.getId());
        resp.put("studentId", cert.getStudentId());
        resp.put("school", cert.getUniversity());
        resp.put("gender", cert.getGender());
        resp.put("age", cert.getAge());
        resp.put("verificationStatus", cert.getCertStatus());
        return resp;
    }

    public Object getCertStatus(Long userId) {
        UserCert cert = userCertRepository.findByUserId(userId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        var resp = new java.util.LinkedHashMap<String, Object>();
        if (user != null) {
            resp.put("userId", user.getId());
            resp.put("username", user.getUsername());
            resp.put("avatar", user.getAvatar());
        }
        if (cert != null) {
            resp.put("certStatus", cert.getCertStatus());
            resp.put("verificationInfo", buildVerificationInfo(cert));
            resp.put("school", cert.getUniversity());
            resp.put("major", cert.getMajor());
            resp.put("grade", cert.getGrade());
            resp.put("bio", cert.getSignature());
            resp.put("interestTags", cert.getInterests());
            resp.put("contactInfo", cert.getContactInfo());
        } else {
            resp.put("certStatus", "UNCERTIFIED");
            resp.put("verificationInfo", null);
        }
        return resp;
    }

    @Transactional
    public Object updateProfile(Long userId, UserProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));
        UserCert cert = userCertRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(40302, "请先完成实名认证"));
        if (!"CERTIFIED".equals(cert.getCertStatus())) {
            throw new BusinessException(40302, "请先完成实名认证");
        }

        String newUsername = req.getNickname().trim();
        if (!newUsername.equals(user.getUsername()) && userRepository.existsByUsername(newUsername)) {
            throw new BusinessException(40901, "该用户名已被占用");
        }

        user.setUsername(newUsername);
        user.setAvatar(StringUtils.hasText(req.getAvatar()) ? req.getAvatar().trim() : null);
        userRepository.save(user);

        cert.setMajor(req.getMajor());
        cert.setGrade(req.getGrade());
        cert.setSignature(req.getBio());
        cert.setInterests(formatTags(req.getInterestTags()));
        cert.setContactInfo(req.getContactInfo());
        userCertRepository.save(cert);

        return buildProfileResponse(user, cert);
    }

    private String maskPhone(String phone) {
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        return phone;
    }

    private java.util.Map<String, Object> buildProfileResponse(User user, UserCert cert) {
        var resp = new java.util.LinkedHashMap<String, Object>();
        resp.put("userId", user.getId());
        resp.put("username", user.getUsername());
        resp.put("nickname", user.getUsername());
        resp.put("avatar", user.getAvatar());
        resp.put("certStatus", cert.getCertStatus());
        resp.put("school", cert.getUniversity());
        resp.put("grade", cert.getGrade());
        resp.put("major", cert.getMajor());
        resp.put("bio", cert.getSignature());
        resp.put("interestTags", cert.getInterests());
        resp.put("contactInfo", cert.getContactInfo());
        return resp;
    }

    private java.util.Map<String, Object> buildVerificationInfo(UserCert cert) {
        var info = new java.util.LinkedHashMap<String, Object>();
        info.put("studentId", cert.getStudentId());
        info.put("school", cert.getUniversity());
        info.put("gender", cert.getGender());
        info.put("age", cert.getAge());
        info.put("verificationStatus", cert.getCertStatus());
        return info;
    }

    private String formatTags(String[] tags) {
        if (tags == null) {
            return null;
        }
        return Arrays.stream(tags)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList()
                .toString();
    }

    private void validateRegisterInput(RegisterRequest req) {
        if (req.getPhone() == null || !PHONE_PATTERN.matcher(req.getPhone()).matches()) {
            throw new BusinessException(40001, "手机号格式不正确");
        }
        if (!StringUtils.hasText(req.getUsername()) || req.getUsername().length() > 16) {
            throw new BusinessException(40001, "用户名不能为空且不能超过16位");
        }
        if (req.getPassword() == null || !PASSWORD_PATTERN.matcher(req.getPassword()).matches()) {
            throw new BusinessException(40001, "密码至少8位且必须包含英文字母和数字");
        }
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException(40003, "两次输入的密码不一致");
        }
    }

    private void verifyCaptcha(String captchaId, String captchaCode) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
            throw new BusinessException(40003, "请输入验证码");
        }
        CaptchaEntry entry = captchaStore.get(captchaId);
        if (entry == null || entry.expiresAt().isBefore(LocalDateTime.now())) {
            captchaStore.remove(captchaId);
            throw new BusinessException(40003, "验证码错误或已过期");
        }
        if (!entry.code().equals(captchaCode)) {
            throw new BusinessException(40003, "验证码错误或已过期");
        }
        captchaStore.remove(captchaId);
    }

    private String generateCaptchaCode() {
        StringBuilder code = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            code.append(CAPTCHA_CHARS.charAt(ThreadLocalRandom.current().nextInt(CAPTCHA_CHARS.length())));
        }
        return code.toString();
    }

    private record CaptchaEntry(String code, LocalDateTime expiresAt) {}
}
