package com.campushub.repository;

import com.campushub.entity.UserCert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserCertRepository extends JpaRepository<UserCert, Long> {
    Optional<UserCert> findByUserId(Long userId);
}
