package com.campushub.repository;

import com.campushub.entity.SensitiveWord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SensitiveWordRepository extends JpaRepository<SensitiveWord, Long> {
    List<SensitiveWord> findAll();
}
