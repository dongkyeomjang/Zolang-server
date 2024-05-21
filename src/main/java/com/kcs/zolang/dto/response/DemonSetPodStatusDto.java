package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "DemonSetPodStatusDto", description = "데몬셋 파드 상태 Dto")
public record DemonSetPodStatusDto(
    @Schema(description = "실행 중인 파드 수")
    Integer runningPods,
    @Schema(description = "원하는 파드 수")
    Integer desiredPods
) {

    public static DemonSetPodStatusDto fromEntity(V1DaemonSet daemonSet) {
        return DemonSetPodStatusDto.builder()
            .runningPods(daemonSet.getStatus().getNumberAvailable() == null ? 0
                : daemonSet.getStatus().getNumberAvailable())
            .desiredPods(daemonSet.getStatus().getDesiredNumberScheduled())
            .build();
    }
}
