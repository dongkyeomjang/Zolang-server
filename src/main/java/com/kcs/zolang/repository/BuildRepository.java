package com.kcs.zolang.repository;

import com.kcs.zolang.domain.Build;
import com.kcs.zolang.domain.CICD;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface BuildRepository extends JpaRepository<Build, Long> {
    @Query("SELECT COALESCE(MAX(b.buildNumber), 0) FROM Build b WHERE b.CICD = :cicd")
    Integer findBuildNumberByCICD(@Param("cicd") CICD cicd);
    List<Build> findByCICD(CICD cicd);
    Optional<Build> findTopByCICDOrderByCreatedAtDesc(CICD cicd);
    Optional<Build> findByCICDId(Long CICDId);
}
