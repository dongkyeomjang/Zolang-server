package com.kcs.zolang.dto.response;

import com.kcs.zolang.domain.Cluster;
import lombok.Builder;

@Builder
public record CreateClusterResponseDto(
        String userName,
        String clusterName,
        String provider,
        String secretToken,
        String domainUrl,
        String version
) {
    public CreateClusterResponseDto fromEntity(Cluster cluster) {
        return CreateClusterResponseDto.builder()
                .userName(cluster.getUser().getNickname())
                .clusterName(cluster.getClusterName())
                .provider(cluster.getProvider())
                .secretToken(cluster.getSecretToken())
                .domainUrl(cluster.getDomainUrl())
                .version(cluster.getVersion())
                .build();
    }
}
