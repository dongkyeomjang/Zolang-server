package com.kcs.zolang.dto.response.cluster;

import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Builder
@Schema(name = "NodeConditionDto", description = "노드 상태 정보 Dto")
public record NodeConditionDto(
        @Schema(description = "lastHeartbeatTime", example = "2024-05-21 12:34:56")
        String lastHeartbeatTime,
        @Schema(description = "lastTransitionTime", example = "2024-05-21 12:34:56")
        String lastTransitionTime,
        @Schema(description = "message", example = "kubelet has sufficient memory available")
        String message,
        @Schema(description = "reason", example = "KubeletHasSufficientMemory")
        String reason,
        @Schema(description = "status", example = "False")
        String status,
        @Schema(description = "type", example = "MemoryPressure")
        String type
) {
    public static NodeConditionDto fromEntity(V1NodeCondition condition) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        return NodeConditionDto.builder()
                .lastHeartbeatTime(formatter.format(condition.getLastHeartbeatTime().toInstant()))
                .lastTransitionTime(formatter.format(condition.getLastTransitionTime().toInstant()))
                .message(condition.getMessage())
                .reason(condition.getReason())
                .status(condition.getStatus())
                .type(condition.getType())
                .build();
    }
}
