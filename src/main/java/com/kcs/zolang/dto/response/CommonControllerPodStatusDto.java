package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "CommonControllerPodStatusDto", description = "데몬셋 파드 상태 Dto")
public record CommonControllerPodStatusDto(
    @Schema(description = "실행 중인 파드 수")
    Integer runningPods,
    @Schema(description = "원하는 파드 수")
    Integer desiredPods
) {

    public static CommonControllerPodStatusDto fromEntity(V1DaemonSet daemonSet) {
        return CommonControllerPodStatusDto.builder()
            .runningPods(daemonSet.getStatus().getNumberAvailable() == null ? 0
                : daemonSet.getStatus().getNumberAvailable())
            .desiredPods(daemonSet.getStatus().getDesiredNumberScheduled())
            .build();
    }

    public static CommonControllerPodStatusDto fromEntity(V1StatefulSet statefulSet) {
        return CommonControllerPodStatusDto.builder()
            .runningPods(statefulSet.getStatus().getCurrentReplicas() == null ? 0
                : statefulSet.getStatus().getCurrentReplicas())
            .desiredPods(statefulSet.getStatus().getReplicas())
            .build();
    }

    public static CommonControllerPodStatusDto fromEntity(V1ReplicaSet replicaSet) {
        return CommonControllerPodStatusDto.builder()
            .runningPods(replicaSet.getStatus().getReadyReplicas() == null ? 0
                : replicaSet.getStatus().getReadyReplicas())
            .desiredPods(replicaSet.getStatus().getReplicas())
            .build();
    }
}
