package com.kcs.zolang.repository;

import com.kcs.zolang.domain.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClusterRepository extends JpaRepository<Cluster,Long> {
    List<Cluster> findByUserId(Long userId);
}
