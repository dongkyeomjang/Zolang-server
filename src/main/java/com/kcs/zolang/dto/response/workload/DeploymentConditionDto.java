package com.kcs.zolang.dto.response.workload;

import static com.kcs.zolang.utility.MonitoringUtil.DATE_TIME_FORMATTER;
import static com.kcs.zolang.utility.MonitoringUtil.getAge;

import io.kubernetes.client.openapi.models.V1DeploymentCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
@Schema(name = "DeploymentConditionDto", description = "디폴로이먼트 상태 조건 Dto")
public record DeploymentConditionDto(
    @Schema(description = "타입", example = "Available")
    String type,
    @Schema(description = "상태", example = "True")
    String status,
    @Schema(description = "마지막 진단 시간", example = "2024.05.21. 오후 12:11:03")
    String lastUpdateTime,
    @Schema(description = "마지막 진단 경과 시간", example = "1 day")
    String lastUpdateAge,
    @Schema(description = "마지막 트랜지션 시간", example = "2024.05.21. 오후 12:11:03")
    String lastTransitionTime,
    @Schema(description = "마지막 트랜지션 경과 시간", example = "1 day")
    String lastTransitionAge,
    @Schema(description = "이유", example = "NewReplicaSetAvailable")
    String reason,
    @Schema(description = "메시지", example = "ReplicaSet \"nginx-deployment-12\" has successfully progressed.")
    String message
) {

    public static DeploymentConditionDto fromEntity(V1DeploymentCondition condition) {
        LocalDateTime lastUpdateTime = condition.getLastUpdateTime() == null ? null
            : condition.getLastUpdateTime().toLocalDateTime();
        LocalDateTime lastTransitionTime = condition.getLastTransitionTime() == null
            ? null : condition.getLastTransitionTime().toLocalDateTime();
        return DeploymentConditionDto.builder()
            .type(condition.getType())
            .status(condition.getStatus())
            .lastUpdateTime(
                lastUpdateTime == null ? null : lastUpdateTime.format(DATE_TIME_FORMATTER))
            .lastUpdateAge(getAge(lastUpdateTime))
            .lastTransitionTime(
                lastTransitionTime == null ? null : lastTransitionTime.format(DATE_TIME_FORMATTER))
            .lastTransitionAge(getAge(lastTransitionTime))
            .reason(condition.getReason())
            .message(condition.getMessage())
            .build();
    }
}
