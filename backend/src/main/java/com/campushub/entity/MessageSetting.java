package com.campushub.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "tb_message_setting")
public class MessageSetting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "system_enabled", nullable = false)
    private boolean systemEnabled = true;

    @Column(name = "interaction_enabled", nullable = false)
    private boolean interactionEnabled = true;

    @Column(name = "alert_type", nullable = false, length = 16)
    private String alertType = "sound";

    @Column(name = "disturb_start")
    private LocalTime disturbStart;

    @Column(name = "disturb_end")
    private LocalTime disturbEnd;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public boolean isSystemEnabled() { return systemEnabled; }
    public void setSystemEnabled(boolean systemEnabled) { this.systemEnabled = systemEnabled; }
    public boolean isInteractionEnabled() { return interactionEnabled; }
    public void setInteractionEnabled(boolean interactionEnabled) { this.interactionEnabled = interactionEnabled; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public LocalTime getDisturbStart() { return disturbStart; }
    public void setDisturbStart(LocalTime disturbStart) { this.disturbStart = disturbStart; }
    public LocalTime getDisturbEnd() { return disturbEnd; }
    public void setDisturbEnd(LocalTime disturbEnd) { this.disturbEnd = disturbEnd; }
}
