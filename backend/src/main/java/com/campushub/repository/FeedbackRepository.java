package com.campushub.repository;

import com.campushub.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Feedback> findByStatus(String status, Pageable pageable);
    Optional<Feedback> findByFeedbackNumber(String feedbackNumber);

    @Query("SELECT f FROM Feedback f WHERE f.userId = :userId AND (:status IS NULL OR f.status = :status)")
    Page<Feedback> findMyFiltered(@Param("userId") Long userId, @Param("status") String status, Pageable pageable);

    @Query("SELECT f FROM Feedback f WHERE (:status IS NULL OR f.status = :status) AND (:type IS NULL OR f.type = :type)")
    Page<Feedback> findAllFiltered(@Param("status") String status, @Param("type") String type, Pageable pageable);
}
