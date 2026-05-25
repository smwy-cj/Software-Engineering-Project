package com.campushub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_privacy_setting")
public class PrivacySetting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "show_real_name", nullable = false, length = 16)
    private String showRealName = "SELF_ONLY";

    @Column(name = "show_student_id", nullable = false, length = 16)
    private String showStudentId = "SELF_ONLY";

    @Column(name = "show_university", nullable = false, length = 16)
    private String showUniversity = "FRIENDS";

    @Column(name = "show_major", nullable = false, length = 16)
    private String showMajor = "FRIENDS";

    @Column(name = "show_age", nullable = false, length = 16)
    private String showAge = "FRIENDS";

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getShowRealName() { return showRealName; }
    public void setShowRealName(String showRealName) { this.showRealName = showRealName; }
    public String getShowStudentId() { return showStudentId; }
    public void setShowStudentId(String showStudentId) { this.showStudentId = showStudentId; }
    public String getShowUniversity() { return showUniversity; }
    public void setShowUniversity(String showUniversity) { this.showUniversity = showUniversity; }
    public String getShowMajor() { return showMajor; }
    public void setShowMajor(String showMajor) { this.showMajor = showMajor; }
    public String getShowAge() { return showAge; }
    public void setShowAge(String showAge) { this.showAge = showAge; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
