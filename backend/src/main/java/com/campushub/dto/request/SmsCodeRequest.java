package com.campushub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SmsCodeRequest {
    @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phone;
    @NotBlank
    private String scene;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
}
