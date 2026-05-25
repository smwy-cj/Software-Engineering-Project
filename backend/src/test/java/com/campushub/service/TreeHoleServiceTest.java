package com.campushub.service;

import com.campushub.entity.TreeHoleLike;
import com.campushub.entity.TreeHolePost;
import com.campushub.entity.User;
import com.campushub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreeHoleServiceTest {

    @Mock private TreeHolePostRepository postRepository;
    @Mock private TreeHoleCommentRepository commentRepository;
    @Mock private TreeHoleLikeRepository likeRepository;
    @Mock private UserRepository userRepository;

    private TreeHoleService treeHoleService;

    @BeforeEach
    void setUp() {
        treeHoleService = new TreeHoleService(postRepository, commentRepository, likeRepository, userRepository);
    }

    @Test
    void createPost_shouldSucceed() {
        User user = new User();
        user.setId(1L);
        user.setUsername("测试用户");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(postRepository.save(any(TreeHolePost.class))).thenAnswer(inv -> {
            TreeHolePost p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        Object result = treeHoleService.createPost(1L, "测试内容", "study", true);
        assertNotNull(result);
    }

    @Test
    void toggleLike_shouldAddLike() {
        TreeHolePost post = new TreeHolePost();
        post.setId(1L);
        post.setLikeCount(5);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(likeRepository.findByPostIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        Object result = treeHoleService.toggleLike(1L, 1L);
        assertNotNull(result);
        assertEquals(6, post.getLikeCount());
    }

    @Test
    void toggleLike_shouldRemoveLike() {
        TreeHolePost post = new TreeHolePost();
        post.setId(1L);
        post.setLikeCount(5);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(likeRepository.findByPostIdAndUserId(1L, 1L)).thenReturn(Optional.of(new TreeHoleLike()));

        Object result = treeHoleService.toggleLike(1L, 1L);
        assertNotNull(result);
        assertEquals(4, post.getLikeCount());
    }
}
