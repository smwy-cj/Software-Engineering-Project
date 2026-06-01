package com.campushub.repository;

import com.campushub.entity.TreeHolePost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TreeHolePostRepository extends JpaRepository<TreeHolePost, Long> {
    @Query("SELECT p FROM TreeHolePost p WHERE p.status = 'PUBLISHED' AND p.isDeleted = false " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:keyword IS NULL OR p.content LIKE %:keyword%)")
    Page<TreeHolePost> findPublicPosts(@Param("category") String category,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);

    Page<TreeHolePost> findByStatusAndIsDeletedFalse(String status, Pageable pageable);

    List<TreeHolePost> findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
}
