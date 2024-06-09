package com.kcs.zolang.repository;

import com.kcs.zolang.domain.Bill;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BillRepository extends JpaRepository<Bill, Long> {

    @Query("SELECT b FROM Bill b WHERE b.date < :thresholdDate")
    List<Bill> findAllByDateBefore(@Param("thresholdDate") String thresholdDate);

    Bill findByUserIdAndDate(Long userId, String string);

    boolean existsByDate(String date);
}
