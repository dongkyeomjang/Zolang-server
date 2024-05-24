package com.kcs.zolang.dto.response.workload;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "DeploymentDetailDto", description = "디폴로이먼트 상세 Dto")
public record DeploymentDetailDto(
    @Schema(description = "deployment 메타데이터")
    CommonMetadataDto metadata,
    @Schema(description = "deployment 리소스")
    DeploymentResourceDto resource,
    @Schema(description = "롤링 업데이트 정책")
    RollingUpdateStrategyDto rollingUpdateStrategy,
    @Schema(description = "pod 상태")
    DeploymentPodStatusDto podConditions,
    @Schema(description = "deployment 상태들")
    List<DeploymentConditionDto> condition
) {

    public static DeploymentDetailDto fromEntity(V1Deployment deployment) {
        return DeploymentDetailDto.builder()
            .metadata(deployment.getMetadata() == null ? null
                : CommonMetadataDto.fromEntity(deployment.getMetadata()))
            .resource(DeploymentResourceDto.fromEntity(deployment))
            .rollingUpdateStrategy(RollingUpdateStrategyDto.fromEntity(deployment))
            .podConditions(DeploymentPodStatusDto.fromEntity(deployment))
            .condition(deployment.getStatus().getConditions().stream()
                .map(DeploymentConditionDto::fromEntity).toList())
            .build();
    }
}
