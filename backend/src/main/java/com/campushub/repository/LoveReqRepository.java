package com.campushub.repository;

import com.campushub.entity.LoveReq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoveReqRepository extends JpaRepository<LoveReq, Long> {
    Page<LoveReq> findByStatus(String status, Pageable pageable);

    List<LoveReq> findByStatus(String status, Sort sort);

    Optional<LoveReq> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    List<LoveReq> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
