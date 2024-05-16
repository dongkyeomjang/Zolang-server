package com.kcs.zolang.repository;

import com.kcs.zolang.domain.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryRepository extends JpaRepository<Repository, Long> {
}
