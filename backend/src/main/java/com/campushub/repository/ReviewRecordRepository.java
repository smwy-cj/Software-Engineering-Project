package com.campushub.repository;

import com.campushub.entity.ReviewRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {
    Page<ReviewRecord> findByContentType(String contentType, Pageable pageable);
}
