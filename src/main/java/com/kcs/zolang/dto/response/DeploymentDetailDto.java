package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "DeploymentDetailDto", description = "배포 상세 Dto")
public record DeploymentDetailDto(
    CommonMetadataDto metadata,
    DeploymentResourceDto resource,
    RolingUpdateStrategyDto rollingUpdateStrategy,
    DeploymentPodStatusDto podConditions,
    List<DeploymentConditionDto> condition
) {

    public static DeploymentDetailDto fromEntity(V1Deployment deployment) {
        return DeploymentDetailDto.builder()
            .metadata(CommonMetadataDto.fromEntity(deployment))
            .resource(DeploymentResourceDto.fromEntity(deployment))
            .rollingUpdateStrategy(RolingUpdateStrategyDto.fromEntity(deployment))
            .podConditions(DeploymentPodStatusDto.fromEntity(deployment))
            .condition(deployment.getStatus().getConditions().stream()
                .map(DeploymentConditionDto::fromEntity).toList())
            .build();
    }
}
