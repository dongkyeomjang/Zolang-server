package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1EnvVar;

public record EnvironmentVariableDto(
    String name,
    String value
) {

    public static EnvironmentVariableDto fromEntity(V1EnvVar envVar) {
        return new EnvironmentVariableDto(envVar.getName(), envVar.getValue());
    }
}
