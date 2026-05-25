package com.campushub.repository;

import com.campushub.entity.TreeHoleLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TreeHoleLikeRepository extends JpaRepository<TreeHoleLike, Long> {
    Optional<TreeHoleLike> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
}
