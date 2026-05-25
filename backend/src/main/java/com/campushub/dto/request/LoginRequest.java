package com.campushub.dto.request;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank private String phone;
    @NotBlank private String loginType;
    private String password;
    private String smsCode;
    private String captcha;
    private String captchaId;
    private boolean rememberMe;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLoginType() { return loginType; }
    public void setLoginType(String loginType) { this.loginType = loginType; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getSmsCode() { return smsCode; }
    public void setSmsCode(String smsCode) { this.smsCode = smsCode; }
    public String getCaptcha() { return captcha; }
    public void setCaptcha(String captcha) { this.captcha = captcha; }
    public String getCaptchaId() { return captchaId; }
    public void setCaptchaId(String captchaId) { this.captchaId = captchaId; }
    public boolean isRememberMe() { return rememberMe; }
    public void setRememberMe(boolean rememberMe) { this.rememberMe = rememberMe; }
}
