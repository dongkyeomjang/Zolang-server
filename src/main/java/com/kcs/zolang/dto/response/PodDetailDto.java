package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1Pod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.Builder;

@Builder
public record PodDetailDto(
    @Schema(description = "Pod 지난 사용량 리스트")
    List<UsageDto> metrics,
    @Schema(description = "Pod 메타데이터")
    CommonMetadataDto metadata,
    @Schema(description = "Pod 리소스 정보")
    PodResourceDto resource,
    @Schema(description = "Pod 조건")
    List<PodConditionsDto> conditions,
    @Schema(description = "Pod  제약")
    PodControlledDto controlled,
    @Nullable
    @Schema(description = "퍼시스턴트 볼륨 클레임")
    List<PodPersistentVolumeClaimDto> persistentVolumeClaims
) {

    public static PodDetailDto fromEntity(V1Pod pod, PodControlledDto controlledDtoList,
        List<PodPersistentVolumeClaimDto> pvcDtoList,
        List<UsageDto> metrics) {
        return PodDetailDto.builder()
            .metrics(metrics)
            .metadata(
                pod.getMetadata() == null ? null : CommonMetadataDto.fromEntity(pod.getMetadata()))
            .resource(PodResourceDto.fromEntity(pod))
            .conditions(
                pod.getStatus().getConditions().stream().map(PodConditionsDto::fromEntity).toList())
            .controlled(controlledDtoList)
            .persistentVolumeClaims(pvcDtoList)
            .build();
    }
}
