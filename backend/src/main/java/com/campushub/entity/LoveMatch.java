package com.campushub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_love_match", uniqueConstraints = @UniqueConstraint(columnNames = {"request_id", "applicant_id"}))
public class LoveMatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "apply_time", nullable = false, updatable = false)
    private LocalDateTime applyTime;

    @Column(name = "response_time")
    private LocalDateTime responseTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @PrePersist void onCreate() { applyTime = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getApplyTime() { return applyTime; }
    public LocalDateTime getResponseTime() { return responseTime; }
    public void setResponseTime(LocalDateTime responseTime) { this.responseTime = responseTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
