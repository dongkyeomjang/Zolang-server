package com.kcs.zolang.dto.response.workload;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1RollingUpdateDeployment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "RollingUpdateStrategyDto", description = "롤링 업데이트 정책 Dto")
public record RollingUpdateStrategyDto(
    @Schema(description = "최대 증가률(%)", example = "25")
    String maxSurge,
    @Schema(description = "최대 비가률(%)", example = "25")
    String maxUnavailable
) {

    public static RollingUpdateStrategyDto fromEntity(V1Deployment deployment) {
        V1RollingUpdateDeployment strategy =
            deployment.getSpec().getStrategy() == null ? null
                : deployment.getSpec().getStrategy().getRollingUpdate();
        return RollingUpdateStrategyDto.builder()
            .maxSurge(
                strategy != null ? strategy.getMaxSurge().toString() : null)
            .maxUnavailable(
                strategy != null ? strategy.getMaxUnavailable().toString()
                    : null)
            .build();
    }
}
