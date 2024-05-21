package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Builder;

@Builder
@Schema(name = "DeploymentResourceDto", description = "디폴로이먼트 리소스 Dto")
public record DeploymentResourceDto(
    @Schema(description = "전략", example = "RollingUpdate")
    String strategy,
    @Schema(description = "최소 준비 시간(초)", example = "0s")
    Integer minimumPreparationTime,
    @Schema(description = "리비전 내역 한도", example = "10")
    Integer revisionHistoryLimit,
    @Schema(description = "셀렉터", example = "k8s-app: kube-dns")
    Map<String, String> selector
) {

    public static DeploymentResourceDto fromEntity(V1Deployment deployment) {
        return DeploymentResourceDto.builder()
            .strategy(deployment.getSpec().getStrategy().getType())
            .minimumPreparationTime(deployment.getSpec().getMinReadySeconds() == null ? 0
                : deployment.getSpec().getMinReadySeconds())
            .revisionHistoryLimit(deployment.getSpec().getRevisionHistoryLimit())
            .selector(deployment.getSpec().getSelector().getMatchLabels())
            .build();
    }
}
