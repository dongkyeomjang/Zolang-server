package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1EnvVar;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "EnvironmentVariableDto", description = "환경 변수 Dto")
public record EnvironmentVariableDto(
    @Schema(description = "환경 변수 이름", example = "env-name")
    String name,
    @Schema(description = "환경 변수 값", example = "env-value")
    String value
) {

    public static EnvironmentVariableDto fromEntity(V1EnvVar envVar) {
        return EnvironmentVariableDto.builder()
            .name(envVar.getName())
            .value(envVar.getValue())
            .build();
    }
}
