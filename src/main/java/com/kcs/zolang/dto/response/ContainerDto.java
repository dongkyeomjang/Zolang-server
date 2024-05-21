package com.kcs.zolang.dto.response;

import static com.kcs.zolang.utility.MonitoringUtil.DATE_TIME_FORMATTER;

import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Volume;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "ContainerDto", description = "컨테이너 정보 Dto")
public record ContainerDto(
    @Schema(description = "컨테이너 실행 여부", example = "true")
    boolean isRunning,
    @Schema(description = "컨테이너 이름", example = "container-name")
    String name,
    @Schema(description = "컨테이너 이미지", example = "image-name")
    String image,
    @Schema(description = "컨테이너 준비 여부", example = "true")
    boolean ready,
    @Schema(description = "컨테이너 시작 여부", example = "true")
    boolean started,
    @Schema(description = "컨테이너 시작 시간", example = "2021. 10. 01. 오후 01:00:00")
    String startedAt,
    @Schema(description = "컨테이너 환경 변수", example = "env")
    List<EnvironmentVariableDto> env,
    @Schema(description = "컨테이너 실행 인자", example = "factor")
    List<String> factor,
    @Schema(description = "컨테이너 마운트 정보", example = "mount")
    List<MountDto> mount,
    @Schema(description = "컨테이너 보안 컨텍스트", example = "securityContext")
    SecurityContextDto securityContext
) {

    public static ContainerDto fromEntity(V1ContainerStatus status, V1Container containers,
        List<V1Volume> v1Volume) {
        return ContainerDto.builder()
            .isRunning(status.getState().getRunning() != null)
            .name(status.getName())
            .image(containers.getImage())
            .ready(status.getReady())
            .started(status.getStarted())
            .startedAt(status.getState().getRunning().getStartedAt().toLocalDateTime().format(
                DATE_TIME_FORMATTER))
            .env(containers.getEnv() == null ? null
                : containers.getEnv().stream().map(EnvironmentVariableDto::fromEntity)
                    .toList())
            .factor(containers.getArgs())
            .mount(containers.getVolumeMounts() == null ? null
                : containers.getVolumeMounts().stream().map(volumeMounts -> MountDto.fromEntity(
                    volumeMounts, v1Volume)).toList())
            .securityContext(containers.getSecurityContext() == null ? null
                : SecurityContextDto.fromEntity(containers.getSecurityContext()))
            .build();
    }
}
