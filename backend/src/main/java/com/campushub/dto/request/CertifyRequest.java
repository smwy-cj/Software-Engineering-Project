package com.campushub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CertifyRequest {
    @NotBlank(message = "学号不能为空")
    @Size(max = 20, message = "学号不能超过20位")
    private String studentId;

    @NotBlank(message = "学校不能为空")
    @Size(max = 64, message = "学校不能超过64位")
    private String school;

    @NotBlank(message = "性别不能为空")
    @Size(max = 8, message = "性别不能超过8位")
    private String gender;

    @Min(value = 16, message = "年龄不能小于16岁")
    @Max(value = 60, message = "年龄不能大于60岁")
    private int age;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}
