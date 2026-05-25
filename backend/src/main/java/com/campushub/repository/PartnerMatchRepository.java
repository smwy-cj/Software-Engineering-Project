package com.campushub.repository;

import com.campushub.entity.PartnerMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PartnerMatchRepository extends JpaRepository<PartnerMatch, Long> {
    Optional<PartnerMatch> findByRequestIdAndApplicantId(Long requestId, Long applicantId);
    boolean existsByRequestIdAndApplicantId(Long requestId, Long applicantId);
    long countByRequestIdAndStatus(Long requestId, String status);

    @Query("SELECT m FROM PartnerMatch m WHERE m.applicantId = :userId OR m.requestId IN " +
           "(SELECT r.id FROM PartnerReq r WHERE r.userId = :userId)")
    Page<PartnerMatch> findByUserId(@Param("userId") Long userId, Pageable pageable);
}
