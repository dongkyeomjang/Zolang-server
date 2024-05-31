package com.kcs.zolang.repository;

import com.kcs.zolang.domain.EnvironmentVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnvVarRepository extends JpaRepository<EnvironmentVariable, Long> {
    List<EnvironmentVariable> findByCICDId(Long cicdId);
}
