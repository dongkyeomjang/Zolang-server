package com.kcs.zolang.repository;

import com.kcs.zolang.domain.Build;
import com.kcs.zolang.domain.CICD;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface BuildRepository extends JpaRepository<Build, Long> {
    Optional<Integer> findBuildNumberByCICD(CICD cicd);
    List<Build> findByCICD(CICD cicd);
    Optional<Build> findTopByCICDOrderByCreatedAtDesc(CICD cicd);
}
