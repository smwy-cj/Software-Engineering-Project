package com.campushub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_feedback")
public class Feedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(length = 512)
    private String images;

    @Column(length = 64)
    private String contact;

    @Column(name = "feedback_number", nullable = false, length = 20, unique = true)
    private String feedbackNumber;

    @Column(nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "process_comment", length = 256)
    private String processComment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getFeedbackNumber() { return feedbackNumber; }
    public void setFeedbackNumber(String feedbackNumber) { this.feedbackNumber = feedbackNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProcessComment() { return processComment; }
    public void setProcessComment(String processComment) { this.processComment = processComment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
