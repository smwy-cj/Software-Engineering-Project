package com.campushub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserProfileRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 16, message = "用户名不能超过16位")
    private String username;

    @Size(max = 1000000, message = "头像图片不能超过1MB")
    private String avatar;

    @NotBlank(message = "性别不能为空")
    @Size(max = 8, message = "性别不能超过8位")
    private String gender;

    @Min(value = 16, message = "年龄不能小于16岁")
    @Max(value = 60, message = "年龄不能大于60岁")
    private int age;

    @Min(value = 2000, message = "入学年份不能早于2000年")
    @Max(value = 2100, message = "入学年份不能晚于2100年")
    private int enrollmentYear;

    @Min(value = 120, message = "身高不能低于120cm")
    @Max(value = 230, message = "身高不能高于230cm")
    private Integer height;

    @NotBlank(message = "专业不能为空")
    @Size(max = 64, message = "专业不能超过64位")
    private String major;

    @Size(max = 100, message = "个性签名不能超过100位")
    private String signature;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public int getEnrollmentYear() { return enrollmentYear; }
    public void setEnrollmentYear(int enrollmentYear) { this.enrollmentYear = enrollmentYear; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}
