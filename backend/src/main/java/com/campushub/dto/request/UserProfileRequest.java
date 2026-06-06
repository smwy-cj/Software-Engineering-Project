package com.campushub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserProfileRequest {
    @NotBlank(message = "昵称不能为空")
    @Size(max = 16, message = "昵称不能超过16位")
    private String nickname;

    @Size(max = 1000000, message = "头像图片不能超过1MB")
    private String avatar;

    @NotBlank(message = "专业不能为空")
    @Size(max = 64, message = "专业不能超过64位")
    private String major;

    @NotBlank(message = "年级不能为空")
    @Size(max = 16, message = "年级不能超过16位")
    private String grade;

    @Size(max = 100, message = "个性签名不能超过100位")
    private String bio;

    private String[] interestTags;

    @Size(max = 100, message = "联系方式不能超过100位")
    private String contactInfo;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String[] getInterestTags() { return interestTags; }
    public void setInterestTags(String[] interestTags) { this.interestTags = interestTags; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
}
