package com.kcs.zolang.dto.response;

import static com.kcs.zolang.service.WorkloadService.getAge;

import io.kubernetes.client.openapi.models.V1PodCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "PodConditionsDto", description = "Pod 상태 조건 Dto")
public record PodConditionsDto(
    String type,
    String status,
    String lastProbeTime,
    String lastTransitionTime,
    String reason,
    String message
) {

    public static PodConditionsDto fromEntity(V1PodCondition condition) {
        return PodConditionsDto.builder()
            .type(condition.getType())
            .status(condition.getStatus())
            .lastProbeTime(getAge(condition.getLastProbeTime().toLocalDateTime()))
            .lastTransitionTime(getAge(condition.getLastTransitionTime().toLocalDateTime()))
            .reason(condition.getReason())
            .message(condition.getMessage())
            .build();
    }

}
