package com.kcs.zolang.repository;

import com.kcs.zolang.domain.EnvVar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnvVarRepository extends JpaRepository<EnvVar, Long> {
    List<EnvVar> findByCICDId(Long cicdId);
}
