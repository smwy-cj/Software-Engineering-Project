package com.campushub.repository;

import com.campushub.entity.TreeHoleComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TreeHoleCommentRepository extends JpaRepository<TreeHoleComment, Long> {
    Page<TreeHoleComment> findByPostIdAndIsDeletedFalse(Long postId, Pageable pageable);
    long countByPostIdAndIsDeletedFalse(Long postId);
}
