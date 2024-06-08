package com.kcs.zolang.repository;

import com.kcs.zolang.domain.Usage;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageRepository extends JpaRepository<Usage, Long> {

    List<Usage> findAllByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start,
        LocalDateTime end);

    // usage 데이터 삭제
    void deleteByCreatedAtBefore(LocalDateTime thresholdDate);

    boolean existsByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
