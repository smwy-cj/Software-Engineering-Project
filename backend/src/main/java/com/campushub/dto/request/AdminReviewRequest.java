package com.campushub.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AdminReviewRequest {
    @NotBlank private String result;
    @NotBlank private String comment;

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
