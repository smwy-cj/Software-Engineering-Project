package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.service.TreeHoleService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/treehole")
public class TreeHoleController {

    private final TreeHoleService treeHoleService;

    public TreeHoleController(TreeHoleService treeHoleService) {
        this.treeHoleService = treeHoleService;
    }

    @GetMapping("/posts")
    public ApiResponse<?> listPosts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "publishTime") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        return ApiResponse.success(treeHoleService.listPosts(category, keyword, sortBy, page, size, userId));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<?> getPostDetail(@PathVariable Long postId,
                                         @RequestAttribute(value = "userId", required = false) Long userId) {
        return ApiResponse.success(treeHoleService.getPostDetail(postId, userId));
    }

    @PostMapping("/posts")
    public ApiResponse<?> createPost(@RequestAttribute("userId") Long userId,
                                      @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        String category = (String) body.get("category");
        boolean allowComment = !(body.get("allowComment") instanceof Boolean b) || b;
        boolean allowLike = !(body.get("allowLike") instanceof Boolean b) || b;
        return ApiResponse.success("发布成功，待审核", treeHoleService.createPost(userId, content, category, allowComment, allowLike));
    }

    @PostMapping("/posts/{postId}/like")
    public ApiResponse<?> toggleLike(@RequestAttribute("userId") Long userId, @PathVariable Long postId) {
        return ApiResponse.success(treeHoleService.toggleLike(userId, postId));
    }

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<?> listComments(@PathVariable Long postId,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(treeHoleService.listComments(postId, page, size));
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<?> addComment(@RequestAttribute("userId") Long userId,
                                      @PathVariable Long postId,
                                      @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        Object parentId = body.get("parentId");
        Long pid = parentId instanceof Number ? ((Number) parentId).longValue() : null;
        return ApiResponse.success("评论成功", treeHoleService.addComment(userId, postId, content, pid));
    }
}
