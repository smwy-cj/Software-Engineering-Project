package com.campushub.repository;

import com.campushub.entity.LoveProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface LoveProfileRepository extends JpaRepository<LoveProfile, Long> {
    Optional<LoveProfile> findByUserId(Long userId);

    @Query("SELECT lp FROM LoveProfile lp WHERE lp.visibility = 'all' " +
           "AND (:gender IS NULL OR lp.gender = :gender) " +
           "AND (:minAge IS NULL OR lp.age >= :minAge) " +
           "AND (:maxAge IS NULL OR lp.age <= :maxAge)")
    Page<LoveProfile> findPublicProfiles(@Param("gender") String gender,
                                         @Param("minAge") Integer minAge,
                                         @Param("maxAge") Integer maxAge,
                                         Pageable pageable);
}
