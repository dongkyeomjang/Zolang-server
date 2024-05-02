package com.kcs.zolang.repository;

import com.kcs.zolang.domain.Cluster;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClusterRepository extends JpaRepository<Cluster, Long>{
    List<Cluster> findByUserId(Long userId);
}
