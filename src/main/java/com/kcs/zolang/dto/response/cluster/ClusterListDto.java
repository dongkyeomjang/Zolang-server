package com.kcs.zolang.dto.response.cluster;

import com.kcs.zolang.domain.Cluster;
import lombok.Builder;

@Builder
public record ClusterListDto(
        Long clusterId,
        String clusterName,
        String domainUrl,
        String version,
        String provider
) {
    public static ClusterListDto fromEntity(Cluster cluster) {
        return ClusterListDto.builder()
                .clusterId(cluster.getId())
                .clusterName(cluster.getClusterName())
                .domainUrl(cluster.getDomainUrl())
                .version(cluster.getVersion())
                .provider(cluster.getProvider())
                .build();
    }
}
