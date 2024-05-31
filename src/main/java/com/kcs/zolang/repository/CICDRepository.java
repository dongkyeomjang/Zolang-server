package com.kcs.zolang.repository;

import com.kcs.zolang.domain.CICD;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface CICDRepository extends JpaRepository<CICD, Long> {
    Optional<CICD> findByRepositoryName(String repositoryName);
    List<CICD> findByUserId(Long userId);
}
