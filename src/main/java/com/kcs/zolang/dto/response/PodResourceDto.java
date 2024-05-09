package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1Pod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "PodResourceDto", description = "Pod 리소스 Dto")
public record PodResourceDto(
    @Schema(description = "Pod 노드", example = "node-name")
    String node,
    @Schema(description = "Pod 상태", example = "Running")
    String status,
    @Schema(description = "Pod IP", example = "127.0.0.1")
    String ip,
    @Schema(description = "Priority Class", example = "Guaranteed")
    String priorityClass,
    @Schema(description = "Pod 재시작 횟수", example = "0")
    List<Integer> restartCount,
    @Schema(description = "서비스 어카운트", example = "default")
    String serviceAccount,
    @Schema(description = "이미지 풀 시크릿", example = "default-secret")
    String imagePullSecret
) {

    public static PodResourceDto fromEntity(V1Pod pod) {
        return PodResourceDto.builder()
            .node(pod.getSpec().getNodeName())
            .status(pod.getStatus().getPhase())
            .ip(pod.getStatus().getPodIP())
            .priorityClass(pod.getSpec().getPriorityClassName())
            .restartCount(pod.getStatus().getContainerStatuses().stream()
                .map(status -> status.getRestartCount()).toList())
            .serviceAccount(pod.getSpec().getServiceAccountName())
            .imagePullSecret(
                pod.getSpec().getImagePullSecrets().stream().map(secret -> secret.getName())
                    .findFirst().orElse(null))
            .build();
    }
}
