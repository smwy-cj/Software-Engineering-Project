package com.campushub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_love_profile")
public class LoveProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 8)
    private String gender;

    @Column(nullable = false)
    private int age;

    private Integer height;
    private Integer weight;

    @Column(length = 8)
    private String constellation;

    @Column(length = 512)
    private String interests;

    @Column(name = "mate_preference", nullable = false, length = 512)
    private String matePreference;

    @Column(nullable = false, length = 100)
    private String declaration;

    @Column(length = 1024)
    private String photos;

    @Column(nullable = false, length = 16)
    private String visibility = "all";

    @Column(nullable = false)
    private int completeness = 0;

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
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
    public String getConstellation() { return constellation; }
    public void setConstellation(String constellation) { this.constellation = constellation; }
    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }
    public String getMatePreference() { return matePreference; }
    public void setMatePreference(String matePreference) { this.matePreference = matePreference; }
    public String getDeclaration() { return declaration; }
    public void setDeclaration(String declaration) { this.declaration = declaration; }
    public String getPhotos() { return photos; }
    public void setPhotos(String photos) { this.photos = photos; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public int getCompleteness() { return completeness; }
    public void setCompleteness(int completeness) { this.completeness = completeness; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
