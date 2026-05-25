package com.campushub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_partner_match", uniqueConstraints = @UniqueConstraint(columnNames = {"request_id", "applicant_id"}))
public class PartnerMatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "apply_message", length = 100)
    private String applyMessage;

    @Column(nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "apply_time", nullable = false, updatable = false)
    private LocalDateTime applyTime;

    @Column(name = "response_time")
    private LocalDateTime responseTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "end_reason", length = 100)
    private String endReason;

    @PrePersist void onCreate() { applyTime = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public String getApplyMessage() { return applyMessage; }
    public void setApplyMessage(String applyMessage) { this.applyMessage = applyMessage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getApplyTime() { return applyTime; }
    public LocalDateTime getResponseTime() { return responseTime; }
    public void setResponseTime(LocalDateTime responseTime) { this.responseTime = responseTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getEndReason() { return endReason; }
    public void setEndReason(String endReason) { this.endReason = endReason; }
}
