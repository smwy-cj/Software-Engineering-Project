package com.campushub.repository;

import com.campushub.entity.LoveReq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoveReqRepository extends JpaRepository<LoveReq, Long> {
    Page<LoveReq> findByStatus(String status, Pageable pageable);
}
