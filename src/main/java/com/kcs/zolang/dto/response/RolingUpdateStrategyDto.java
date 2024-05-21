package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1RollingUpdateDeployment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "RolingUpdateStrategyDto", description = "롤링 업데이트 정책 Dto")
public record RolingUpdateStrategyDto(
    @Schema(description = "최대 증가률(%)", example = "25")
    String maxSurge,
    @Schema(description = "최대 비가률(%)", example = "25")
    String maxUnavailable
) {

    public static RolingUpdateStrategyDto fromEntity(V1Deployment deployment) {
        V1RollingUpdateDeployment strategy =
            deployment.getSpec().getStrategy() == null ? null
                : deployment.getSpec().getStrategy().getRollingUpdate();
        return RolingUpdateStrategyDto.builder()
            .maxSurge(
                strategy != null ? strategy.getMaxSurge().toString() : null)
            .maxUnavailable(
                strategy != null ? strategy.getMaxUnavailable().toString()
                    : null)
            .build();
    }
}
