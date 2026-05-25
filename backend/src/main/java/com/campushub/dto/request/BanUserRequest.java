package com.campushub.dto.request;

import jakarta.validation.constraints.NotBlank;

public class BanUserRequest {
    @NotBlank private String action;
    @NotBlank private String reason;
    private Integer duration;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
}
