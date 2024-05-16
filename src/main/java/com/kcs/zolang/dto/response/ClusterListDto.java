package com.kcs.zolang.dto.response;

import com.kcs.zolang.domain.Cluster;
import lombok.Builder;
import lombok.Getter;

@Builder
public record ClusterListDto(
        Long clusterId,
        String clusterName,
        String domainUrl,
        String version
) {
    public static ClusterListDto fromEntity(Cluster cluster) {
        return ClusterListDto.builder()
                .clusterId(cluster.getId())
                .clusterName(cluster.getClusterName())
                .domainUrl(cluster.getDomainUrl())
                .version(cluster.getVersion())
                .build();
    }
}
