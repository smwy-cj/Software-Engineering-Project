package com.campushub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_user_cert")
public class UserCert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "student_id", nullable = false, length = 20, unique = true)
    private String studentId;

    @Column(name = "real_name", nullable = false, length = 32)
    private String realName;

    @Column(name = "id_card", nullable = false, length = 18)
    private String idCard;

    @Column(nullable = false, length = 64)
    private String university;

    @Column(nullable = false, length = 64)
    private String major;

    @Column(nullable = false, length = 16)
    private String grade;

    @Column(nullable = false, length = 8)
    private String gender;

    @Column(nullable = false)
    private int age;

    @Column(length = 512)
    private String interests;

    @Column(name = "cert_status", nullable = false, length = 16)
    private String certStatus = "UNCERTIFIED";

    @Column(name = "cert_attempts", nullable = false)
    private int certAttempts = 0;

    @Column(name = "face_verified", nullable = false)
    private boolean faceVerified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }
    public String getCertStatus() { return certStatus; }
    public void setCertStatus(String certStatus) { this.certStatus = certStatus; }
    public int getCertAttempts() { return certAttempts; }
    public void setCertAttempts(int certAttempts) { this.certAttempts = certAttempts; }
    public boolean isFaceVerified() { return faceVerified; }
    public void setFaceVerified(boolean faceVerified) { this.faceVerified = faceVerified; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
