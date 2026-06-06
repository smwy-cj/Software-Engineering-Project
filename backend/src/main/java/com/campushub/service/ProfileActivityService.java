package com.campushub.service;

import com.campushub.entity.LoveMatch;
import com.campushub.entity.LoveReq;
import com.campushub.entity.PartnerMatch;
import com.campushub.entity.PartnerReq;
import com.campushub.entity.TreeHolePost;
import com.campushub.repository.LoveMatchRepository;
import com.campushub.repository.LoveReqRepository;
import com.campushub.repository.NotificationRepository;
import com.campushub.repository.PartnerMatchRepository;
import com.campushub.repository.PartnerReqRepository;
import com.campushub.repository.TreeHolePostRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProfileActivityService {
    private final TreeHolePostRepository treeHolePostRepository;
    private final PartnerReqRepository partnerReqRepository;
    private final LoveReqRepository loveReqRepository;
    private final PartnerMatchRepository partnerMatchRepository;
    private final LoveMatchRepository loveMatchRepository;
    private final NotificationRepository notificationRepository;

    public ProfileActivityService(TreeHolePostRepository treeHolePostRepository,
                                  PartnerReqRepository partnerReqRepository,
                                  LoveReqRepository loveReqRepository,
                                  PartnerMatchRepository partnerMatchRepository,
                                  LoveMatchRepository loveMatchRepository,
                                  NotificationRepository notificationRepository) {
        this.treeHolePostRepository = treeHolePostRepository;
        this.partnerReqRepository = partnerReqRepository;
        this.loveReqRepository = loveReqRepository;
        this.partnerMatchRepository = partnerMatchRepository;
        this.loveMatchRepository = loveMatchRepository;
        this.notificationRepository = notificationRepository;
    }

    public Map<String, Object> getStats(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("treeHoleCount", treeHolePostRepository.countByUserIdAndIsDeletedFalse(userId));
        result.put("receivedLikes", treeHolePostRepository.sumLikeCountByUserId(userId));
        result.put("receivedComments", treeHolePostRepository.sumCommentCountByUserId(userId));
        result.put("unreadNotifications", notificationRepository.countByUserIdAndIsReadFalse(userId));
        return result;
    }

    public Map<String, Object> listPublished(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("treeHole", treeHolePostRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::treeHoleItem).toList());
        result.put("partner", partnerReqRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::partnerItem).toList());
        result.put("love", loveReqRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::loveItem).toList());
        return result;
    }

    public Map<String, Object> listApplications(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("partner", partnerMatchRepository.findTop20ByApplicantIdOrderByApplyTimeDesc(userId)
                .stream().map(this::partnerApplicationItem).toList());
        result.put("love", loveMatchRepository.findTop20ByApplicantIdOrderByApplyTimeDesc(userId)
                .stream().map(this::loveApplicationItem).toList());
        return result;
    }

    private Map<String, Object> treeHoleItem(TreeHolePost post) {
        Map<String, Object> item = baseItem("treeHole", post.getId(), post.getStatus(), post.getCreatedAt());
        item.put("title", categoryName(post.getCategory()));
        item.put("content", post.getContent());
        item.put("meta", "点赞 " + post.getLikeCount() + " · 评论 " + post.getCommentCount());
        return item;
    }

    private Map<String, Object> partnerItem(PartnerReq req) {
        Map<String, Object> item = baseItem("partner", req.getId(), req.getStatus(), req.getCreatedAt());
        item.put("title", partnerTypeName(req.getType()));
        item.put("content", req.getDescription());
        item.put("meta", "最多 " + req.getMaxMembers() + " 人 · 有效 " + req.getValidDays() + " 天");
        return item;
    }

    private Map<String, Object> loveItem(LoveReq req) {
        Map<String, Object> item = baseItem("love", req.getId(), req.getStatus(), req.getCreatedAt());
        item.put("title", "交友需求");
        item.put("content", req.getDescription());
        item.put("meta", "范围 " + req.getScope() + " · 有效 " + req.getValidDays() + " 天");
        return item;
    }

    private Map<String, Object> partnerApplicationItem(PartnerMatch match) {
        PartnerReq req = partnerReqRepository.findById(match.getRequestId()).orElse(null);
        Map<String, Object> item = baseItem("partner", match.getId(), match.getStatus(), match.getApplyTime());
        item.put("title", "搭子申请");
        item.put("content", req != null ? req.getDescription() : "原搭子需求已不可用");
        item.put("meta", match.getApplyMessage() != null ? match.getApplyMessage() : "无申请附言");
        return item;
    }

    private Map<String, Object> loveApplicationItem(LoveMatch match) {
        LoveReq req = loveReqRepository.findById(match.getRequestId()).orElse(null);
        Map<String, Object> item = baseItem("love", match.getId(), match.getStatus(), match.getApplyTime());
        item.put("title", "心动申请");
        item.put("content", req != null ? req.getDescription() : "原交友需求已不可用");
        item.put("meta", "状态 " + statusText(match.getStatus()));
        return item;
    }

    private Map<String, Object> baseItem(String module, Long id, String status, Object createdAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("module", module);
        item.put("id", id);
        item.put("status", status);
        item.put("statusText", statusText(status));
        item.put("createdAt", createdAt);
        return item;
    }

    private String statusText(String status) {
        return switch (status == null ? "" : status) {
            case "PUBLISHED" -> "已发布";
            case "PENDING" -> "审核中";
            case "APPROVED" -> "已通过";
            case "ACCEPTED" -> "已接受";
            case "REJECTED" -> "已拒绝";
            case "CANCELED" -> "已取消";
            case "CLOSED" -> "已关闭";
            case "EXPIRED" -> "已过期";
            case "COMPLETED" -> "已完成";
            case "ENDED" -> "已结束";
            case "DRAFT" -> "草稿";
            default -> "处理中";
        };
    }

    private String categoryName(String category) {
        return switch (category == null ? "" : category) {
            case "study" -> "学习树洞";
            case "life" -> "生活树洞";
            case "fun" -> "娱乐树洞";
            default -> "树洞发布";
        };
    }

    private String partnerTypeName(String type) {
        return switch (type == null ? "" : type) {
            case "study" -> "学习搭子";
            case "sport" -> "运动搭子";
            case "meal" -> "饭搭子";
            default -> "搭子需求";
        };
    }
}
