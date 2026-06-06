package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.entity.TreeHoleLike;
import com.campushub.entity.TreeHolePost;
import com.campushub.entity.User;
import com.campushub.entity.UserCert;
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
    @Mock private UserCertRepository userCertRepository;
    @Mock private ContentReviewService contentReviewService;
    @Mock private NotificationService notificationService;

    private TreeHoleService treeHoleService;

    @BeforeEach
    void setUp() {
        treeHoleService = new TreeHoleService(postRepository, commentRepository, likeRepository,
                userRepository, userCertRepository, contentReviewService, notificationService);
    }

    @Test
    void createPost_shouldCreatePendingReviewForCertifiedUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("测试用户");
        user.setAccountStatus("NORMAL");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserCert cert = new UserCert();
        cert.setCertStatus("CERTIFIED");
        when(userCertRepository.findByUserId(1L)).thenReturn(Optional.of(cert));

        when(postRepository.save(any(TreeHolePost.class))).thenAnswer(inv -> {
            TreeHolePost p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        Object result = treeHoleService.createPost(1L, "这是一条满足长度要求的测试内容", "study", true, true);
        assertNotNull(result);
        verify(postRepository).save(argThat(post ->
                "PENDING".equals(post.getStatus()) && post.getAnonymousName().startsWith("匿名小友")));
        verify(contentReviewService).submitForReview("treeholePost", 1L, 1L, "这是一条满足长度要求的测试内容");
    }

    @Test
    void createPost_shouldRejectUncertifiedUser() {
        User user = new User();
        user.setId(1L);
        user.setAccountStatus("NORMAL");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userCertRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> treeHoleService.createPost(1L, "这是一条满足长度要求的测试内容", "study", true, true));
    }

    @Test
    void addComment_shouldUseAnonymousNameInsteadOfUsername() {
        TreeHolePost post = new TreeHolePost();
        post.setId(1L);
        post.setUserId(2L);
        post.setStatus("PUBLISHED");
        post.setCommentEnabled(true);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        User user = new User();
        user.setId(1L);
        user.setUsername("真实用户名");
        user.setAccountStatus("NORMAL");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserCert cert = new UserCert();
        cert.setCertStatus("CERTIFIED");
        when(userCertRepository.findByUserId(1L)).thenReturn(Optional.of(cert));
        when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        treeHoleService.addComment(1L, 1L, "评论内容", null);

        verify(commentRepository).save(argThat(comment ->
                comment.getAnonymousName().startsWith("匿名小友")
                        && !"真实用户名".equals(comment.getAnonymousName())));
        verify(notificationService).createNotification(eq(2L), eq("treehole_comment"),
                anyString(), contains("评论内容"), eq("treeholePost"), eq(1L));
    }

    @Test
    void toggleLike_shouldRejectWhenPostDisablesLikes() {
        TreeHolePost post = new TreeHolePost();
        post.setId(1L);
        post.setLikeEnabled(false);
        post.setStatus("PUBLISHED");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThrows(BusinessException.class, () -> treeHoleService.toggleLike(1L, 1L));
    }

    @Test
    void toggleLike_shouldAddLike() {
        TreeHolePost post = new TreeHolePost();
        post.setId(1L);
        post.setUserId(2L);
        post.setLikeCount(5);
        post.setLikeEnabled(true);
        post.setStatus("PUBLISHED");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        User user = new User();
        user.setId(1L);
        user.setAccountStatus("NORMAL");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserCert cert = new UserCert();
        cert.setCertStatus("CERTIFIED");
        when(userCertRepository.findByUserId(1L)).thenReturn(Optional.of(cert));
        when(likeRepository.findByPostIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        Object result = treeHoleService.toggleLike(1L, 1L);
        assertNotNull(result);
        assertEquals(6, post.getLikeCount());
        verify(notificationService).createNotification(eq(2L), eq("treehole_like"),
                anyString(), anyString(), eq("treeholePost"), eq(1L));
    }

    @Test
    void toggleLike_shouldRemoveLike() {
        TreeHolePost post = new TreeHolePost();
        post.setId(1L);
        post.setLikeCount(5);
        post.setLikeEnabled(true);
        post.setStatus("PUBLISHED");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        User user = new User();
        user.setId(1L);
        user.setAccountStatus("NORMAL");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserCert cert = new UserCert();
        cert.setCertStatus("CERTIFIED");
        when(userCertRepository.findByUserId(1L)).thenReturn(Optional.of(cert));
        when(likeRepository.findByPostIdAndUserId(1L, 1L)).thenReturn(Optional.of(new TreeHoleLike()));

        Object result = treeHoleService.toggleLike(1L, 1L);
        assertNotNull(result);
        assertEquals(4, post.getLikeCount());
    }
}
