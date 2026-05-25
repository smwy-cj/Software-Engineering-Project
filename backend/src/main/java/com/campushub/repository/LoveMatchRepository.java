package com.campushub.repository;

import com.campushub.entity.LoveMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface LoveMatchRepository extends JpaRepository<LoveMatch, Long> {
    Optional<LoveMatch> findByRequestIdAndApplicantId(Long requestId, Long applicantId);

    @Query("SELECT m FROM LoveMatch m WHERE m.applicantId = :userId OR m.requestId IN " +
           "(SELECT r.id FROM LoveReq r WHERE r.userId = :userId)")
    Page<LoveMatch> findByUserId(@Param("userId") Long userId, Pageable pageable);
}
