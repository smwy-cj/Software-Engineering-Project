package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.dto.request.PartnerReqRequest;
import com.campushub.dto.request.ReviewSubmitRequest;
import com.campushub.service.PartnerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/partner")
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @PostMapping("/requests")
    public ApiResponse<?> createRequest(@RequestAttribute("userId") Long userId,
                                         @Valid @RequestBody PartnerReqRequest req) {
        return ApiResponse.success("搭子需求发布成功", partnerService.createRequest(userId, req));
    }

    @GetMapping("/requests")
    public ApiResponse<?> listRequests(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "publishTime") String sortBy) {
        return ApiResponse.success(partnerService.listRequests(type, keyword, page, size, sortBy));
    }

    @PostMapping("/requests/{requestId}/apply")
    public ApiResponse<?> applyMatch(@RequestAttribute("userId") Long userId,
                                      @PathVariable Long requestId,
                                      @RequestBody Map<String, String> body) {
        return ApiResponse.success("匹配申请已发送",
                partnerService.applyMatch(userId, requestId, body.get("message")));
    }

    @PutMapping("/requests/{requestId}/cancel")
    public ApiResponse<?> cancelRequest(@RequestAttribute("userId") Long userId,
                                        @PathVariable Long requestId,
                                        @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ApiResponse.success("搭子需求已撤销",
                partnerService.cancelRequest(userId, requestId, reason));
    }

    @GetMapping("/matches")
    public ApiResponse<?> listMatches(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(partnerService.listMatches(userId, status, page, size));
    }

    @GetMapping("/matches/{matchId}")
    public ApiResponse<?> getMatchDetail(@RequestAttribute("userId") Long userId,
                                           @PathVariable Long matchId) {
        return ApiResponse.success(partnerService.getMatchDetail(userId, matchId));
    }

    @PutMapping("/matches/{matchId}")
    public ApiResponse<?> updateMatchStatus(@RequestAttribute("userId") Long userId,
                                            @PathVariable Long matchId,
                                            @RequestBody Map<String, String> body) {
        return ApiResponse.success("匹配状态已更新",
                partnerService.updateMatchStatus(userId, matchId, body.get("status"), body.get("reason")));
    }

    @PostMapping("/matches/{matchId}/reviews")
    public ApiResponse<?> submitReview(@RequestAttribute("userId") Long userId,
                                        @PathVariable Long matchId,
                                        @Valid @RequestBody ReviewSubmitRequest req) {
        return ApiResponse.success("评价提交成功", partnerService.submitReview(userId, matchId, req));
    }
}
