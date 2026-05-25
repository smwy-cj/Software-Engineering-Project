package com.campushub.repository;

import com.campushub.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Feedback> findByStatus(String status, Pageable pageable);
    Optional<Feedback> findByFeedbackNumber(String feedbackNumber);
}
