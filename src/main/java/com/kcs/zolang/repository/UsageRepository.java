package com.kcs.zolang.repository;

import com.kcs.zolang.domain.Usage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface UsageRepository extends JpaRepository<Usage, Long> {
    List<Usage> findAllByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    // usage 데이터 삭제
    void deleteByCreatedAtBefore(LocalDateTime thresholdDate);
}
