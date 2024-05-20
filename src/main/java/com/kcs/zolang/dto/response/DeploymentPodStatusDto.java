package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "DeploymentPodStatusDto", description = "디폴로이먼트 Pod 상태 Dto")
public record DeploymentPodStatusDto(
    @Schema(description = "업데이트된 레플리카 수", example = "1")
    Integer updatedReplicas,
    @Schema(description = "레플리카 수", example = "1")
    Integer totalReplicas,
    @Schema(description = "사용 가능한 레플리카 수", example = "1")
    Integer availableReplicas
) {

    public static DeploymentPodStatusDto fromEntity(V1Deployment deployment) {
        return DeploymentPodStatusDto.builder()
            .updatedReplicas(deployment.getStatus().getUpdatedReplicas())
            .totalReplicas(deployment.getStatus().getReplicas())
            .availableReplicas(deployment.getStatus().getAvailableReplicas())
            .build();
    }
}
