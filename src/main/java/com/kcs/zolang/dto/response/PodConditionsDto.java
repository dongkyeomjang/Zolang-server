package com.kcs.zolang.dto.response;

import static com.kcs.zolang.utility.MonitoringUtil.getAge;

import io.kubernetes.client.openapi.models.V1PodCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;
import lombok.Builder;

@Builder
@Schema(name = "PodConditionsDto", description = "Pod 상태 조건 Dto")
public record PodConditionsDto(
    @Schema(description = "조건 종류", example = "Ready")
    String type,
    @Schema(description = "조건 상태", example = "True")
    String status,
    @Schema(description = "마지막 프로브 시간")
    @Nullable
    String lastProbeTime,
    @Schema(description = "마지막 전환 시간")
    String lastTransitionTime,
    @Schema(description = "조건 원인")
    String reason,
    @Schema(description = "조건 메시지")
    String message
) {

    public static PodConditionsDto fromEntity(V1PodCondition condition) {
        return PodConditionsDto.builder()
            .type(condition.getType())
            .status(condition.getStatus())
            .lastProbeTime(getAge(condition.getLastProbeTime() == null ? null
                : condition.getLastProbeTime().toLocalDateTime()))
            .lastTransitionTime(getAge(
                condition.getLastTransitionTime().toLocalDateTime() == null ? null
                    : condition.getLastTransitionTime().toLocalDateTime()))
            .reason(condition.getReason())
            .message(condition.getMessage())
            .build();
    }

}
