package com.kcs.zolang.dto.response;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Pod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.Builder;

@Builder
public record PodDto(
    @Schema(description = "Pod 메타데이터")
    PodMetadataDto metadata,
    @Schema(description = "Pod CPU", example = "0.5")
    @Nullable
    Double cpu,
    @Schema(description = "Pod 메모리", example = "512")
    @Nullable
    Long memory,
    @Schema(description = "Pod 리소스 정보")
    PodResourceDto resource,
    @Schema(description = "Pod 조건")
    List<PodConditionsDto> conditions
) {

    public static PodDto fromEntity(V1Pod pod, String age) {
        Quantity cpuLimit = pod.getSpec().getContainers().get(0).getResources().getLimits()
            .get("cpu");
        Quantity memoryLimit = pod.getSpec().getContainers().get(0).getResources().getLimits()
            .get("memory");
        return PodDto.builder()
            .metadata(PodMetadataDto.fromEntity(pod, age))
            .cpu(cpuLimit.getNumber().doubleValue())
            .memory(memoryLimit.getNumber().longValue())
            .resource(PodResourceDto.fromEntity(pod))
            .conditions(
                pod.getStatus().getConditions().stream().map(PodConditionsDto::fromEntity).toList())
            .build();
    }
}
