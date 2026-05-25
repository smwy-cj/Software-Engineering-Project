package com.campushub.repository;

import com.campushub.entity.PartnerReview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PartnerReviewRepository extends JpaRepository<PartnerReview, Long> {
    Optional<PartnerReview> findByMatchIdAndReviewerId(Long matchId, Long reviewerId);
    boolean existsByMatchIdAndReviewerId(Long matchId, Long reviewerId);
}
