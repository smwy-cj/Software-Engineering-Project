package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.common.PageResult;
import com.campushub.entity.*;
import com.campushub.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TreeHoleService {

    private final TreeHolePostRepository postRepository;
    private final TreeHoleCommentRepository commentRepository;
    private final TreeHoleLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final UserCertRepository userCertRepository;
    private final ContentReviewService contentReviewService;
    private final NotificationService notificationService;

    public TreeHoleService(TreeHolePostRepository postRepository,
                           TreeHoleCommentRepository commentRepository,
                           TreeHoleLikeRepository likeRepository,
                           UserRepository userRepository,
                           UserCertRepository userCertRepository,
                           ContentReviewService contentReviewService,
                           NotificationService notificationService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
        this.userCertRepository = userCertRepository;
        this.contentReviewService = contentReviewService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Object createPost(Long userId, String content, String category, boolean allowComment, boolean allowLike) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));
        checkCanPublish(user);
        requireLength(content, 10, 800, "树洞内容");

        TreeHolePost post = new TreeHolePost();
        post.setUserId(userId);
        post.setContent(content);
        post.setCategory(category != null ? category : "other");
        post.setAnonymousName(generateAnonymousName());
        post.setCommentEnabled(allowComment);
        post.setLikeEnabled(allowLike);
        post.setStatus("PENDING");
        post = postRepository.save(post);
        contentReviewService.submitForReview("treeholePost", post.getId(), userId, content);

        var resp = new LinkedHashMap<String, Object>();
        resp.put("postId", post.getId());
        resp.put("status", post.getStatus());
        resp.put("createdAt", post.getCreatedAt());
        return resp;
    }

    public PageResult<Map<String, Object>> listPosts(String category, String keyword, String sortBy, int page, int size, Long currentUserId) {
        Sort sort;
        if ("hot".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "likeCount");
        } else {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<TreeHolePost> result = postRepository.findPublicPosts(category, keyword, pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (TreeHolePost post : result.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("postId", post.getId());
            item.put("anonymousName", post.getAnonymousName());
            item.put("content", post.getContent());
            item.put("category", post.getCategory());
            item.put("images", post.getImages());
            item.put("likeCount", post.getLikeCount());
            item.put("commentCount", post.getCommentCount());
            item.put("createdAt", post.getCreatedAt());
            item.put("likedByMe", currentUserId != null &&
                    likeRepository.existsByPostIdAndUserId(post.getId(), currentUserId));
            content.add(item);
        }

        return new PageResult<>(content, page, size, result.getTotalElements());
    }

    public Object getPostDetail(Long postId, Long currentUserId) {
        TreeHolePost post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(40401, "动态不存在"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("postId", post.getId());
        data.put("anonymousName", post.getAnonymousName());
        data.put("content", post.getContent());
        data.put("category", post.getCategory());
        data.put("images", post.getImages());
        data.put("likeCount", post.getLikeCount());
        data.put("commentCount", post.getCommentCount());
        data.put("createdAt", post.getCreatedAt());
        data.put("likedByMe", currentUserId != null &&
                likeRepository.existsByPostIdAndUserId(postId, currentUserId));
        return data;
    }

    @Transactional
    public Object toggleLike(Long userId, Long postId) {
        TreeHolePost post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(40401, "动态不存在"));
        if (!"PUBLISHED".equals(post.getStatus())) {
            throw new BusinessException(40401, "动态不存在");
        }
        if (!post.isLikeEnabled()) {
            throw new BusinessException(40003, "该动态不允许点赞");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));
        checkCanPublish(user);

        Optional<TreeHoleLike> existing = likeRepository.findByPostIdAndUserId(postId, userId);
        boolean liked;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            liked = false;
        } else {
            TreeHoleLike like = new TreeHoleLike();
            like.setPostId(postId);
            like.setUserId(userId);
            likeRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);
            liked = true;
            if (!post.getUserId().equals(userId)) {
                notificationService.createNotification(post.getUserId(), "treehole_like",
                        "树洞收到点赞", "你的树洞收到了新的点赞", "treeholePost", postId);
            }
        }
        postRepository.save(post);

        var resp = new LinkedHashMap<String, Object>();
        resp.put("liked", liked);
        resp.put("likeCount", post.getLikeCount());
        return resp;
    }

    @Transactional
    public Object addComment(Long userId, Long postId, String content, Long parentId) {
        TreeHolePost post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(40401, "动态不存在"));
        if (!"PUBLISHED".equals(post.getStatus())) {
            throw new BusinessException(40401, "动态不存在");
        }
        if (!post.isCommentEnabled()) {
            throw new BusinessException(40003, "该动态不允许评论");
        }
        requireLength(content, 1, 100, "评论内容");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));
        checkCanPublish(user);

        TreeHoleComment comment = new TreeHoleComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setAnonymousName(generateAnonymousName());
        comment.setStatus("PUBLISHED");
        comment = commentRepository.save(comment);

        post.setCommentCount((int) commentRepository.countByPostIdAndIsDeletedFalse(postId));
        postRepository.save(post);
        if (!post.getUserId().equals(userId)) {
            String summary = content.length() > 60 ? content.substring(0, 60) + "..." : content;
            notificationService.createNotification(post.getUserId(), "treehole_comment",
                    "树洞收到评论", "你的树洞收到了新评论：" + summary, "treeholePost", postId);
        }

        var resp = new LinkedHashMap<String, Object>();
        resp.put("commentId", comment.getId());
        resp.put("createdAt", comment.getCreatedAt());
        return resp;
    }

    public PageResult<Map<String, Object>> listComments(Long postId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<TreeHoleComment> result = commentRepository.findByPostIdAndIsDeletedFalse(postId, pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (TreeHoleComment c : result.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("commentId", c.getId());
            item.put("anonymousName", c.getAnonymousName());
            item.put("content", c.getContent());
            item.put("parentId", c.getParentId());
            item.put("createdAt", c.getCreatedAt());
            content.add(item);
        }
        return new PageResult<>(content, page, size, result.getTotalElements());
    }

    private String generateAnonymousName() {
        return "匿名小友" + (100 + new Random().nextInt(900));
    }

    private void checkCanPublish(User user) {
        if ("BANNED".equals(user.getAccountStatus())) {
            throw new BusinessException(40103, "账号已被封禁");
        }
        if ("MUTED".equals(user.getAccountStatus())) {
            throw new BusinessException(40303, "您当前处于禁言期，无法发布及互动内容");
        }
        UserCert cert = userCertRepository.findByUserId(user.getId()).orElse(null);
        if (cert == null || !"CERTIFIED".equals(cert.getCertStatus())) {
            throw new BusinessException(40302, "请先完成实名认证");
        }
    }

    private void requireLength(String value, int min, int max, String field) {
        int length = value == null ? 0 : value.trim().length();
        if (length < min || length > max) {
            throw new BusinessException(40001, field + "长度需为" + min + "-" + max + "字");
        }
    }
}
