package com.kcs.zolang.repository;

import com.kcs.zolang.domain.Bill;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {

    @Query("SELECT b FROM Bill b WHERE b.date < :thresholdDate")
    List<Bill> findAllByDateBefore(@Param("thresholdDate") String thresholdDate);

    Bill findByUserIdAndDate(Long userId, String string);
}
