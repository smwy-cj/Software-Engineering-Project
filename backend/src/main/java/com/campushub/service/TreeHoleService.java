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

    public TreeHoleService(TreeHolePostRepository postRepository,
                           TreeHoleCommentRepository commentRepository,
                           TreeHoleLikeRepository likeRepository,
                           UserRepository userRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Object createPost(Long userId, String content, String category, boolean anonymous) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));

        TreeHolePost post = new TreeHolePost();
        post.setUserId(userId);
        post.setContent(content);
        post.setCategory(category != null ? category : "other");
        post.setAnonymousName(anonymous ? generateAnonymousName() : user.getUsername());
        post.setStatus("PUBLISHED");
        post = postRepository.save(post);

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

        User user = userRepository.findById(userId).orElse(null);

        TreeHoleComment comment = new TreeHoleComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setAnonymousName(user != null ? user.getUsername() : "匿名用户");
        comment.setStatus("PUBLISHED");
        comment = commentRepository.save(comment);

        post.setCommentCount((int) commentRepository.countByPostIdAndIsDeletedFalse(postId));
        postRepository.save(post);

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
}
