package com.campushub.repository;

import com.campushub.entity.PartnerReq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartnerReqRepository extends JpaRepository<PartnerReq, Long> {
    @Query("SELECT p FROM PartnerReq p WHERE p.status = 'PUBLISHED' AND p.isDeleted = false " +
           "AND (:type IS NULL OR p.type = :type) " +
           "AND (:keyword IS NULL OR p.description LIKE %:keyword%)")
    Page<PartnerReq> findPublicRequests(@Param("type") String type,
                                        @Param("keyword") String keyword,
                                        Pageable pageable);
}
