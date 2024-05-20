package com.kcs.zolang.dto.response;

import static com.kcs.zolang.utility.MonitoringUtil.DATE_TIME_FORMATTER;

import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Volume;
import java.util.List;
import lombok.Builder;

@Builder
public record ContainerDto(
    boolean isRunning,
    String name,
    String image,
    boolean ready,
    boolean started,
    String startedAt,
    List<EnvironmentVariableDto> env,
    List<String> factor,
    List<MountDto> mount,
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
