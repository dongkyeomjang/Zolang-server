package com.kcs.zolang.repository;

import com.kcs.zolang.domain.CICD;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CICDRepository extends JpaRepository<CICD, Long> {
    Optional<CICD> findByRepositoryName(String repositoryName);
    List<CICD> findByUserId(Long userId);
}
