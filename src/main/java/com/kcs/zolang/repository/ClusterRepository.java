package com.kcs.zolang.repository;

import com.kcs.zolang.domain.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClusterRepository extends JpaRepository<Cluster, Long>{
    Cluster findByUserId(Long user_id);
}
