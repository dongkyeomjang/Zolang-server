package com.kcs.zolang.dto.response;

import com.kcs.zolang.domain.Cluster;
import lombok.Builder;

@Builder
public record ClusterCreateResponseDto(
        String userName,
        String clusterName,
        String provider,
        String secretToken,
        String domainUrl,
        String version
) {
    public static ClusterCreateResponseDto fromEntity(Cluster cluster) {
        return ClusterCreateResponseDto.builder()
                .userName(cluster.getUser().getNickname())
                .clusterName(cluster.getClusterName())
                .provider(cluster.getProvider())
                .secretToken(cluster.getSecretToken())
                .domainUrl(cluster.getDomainUrl())
                .version(cluster.getVersion())
                .build();
    }
}
