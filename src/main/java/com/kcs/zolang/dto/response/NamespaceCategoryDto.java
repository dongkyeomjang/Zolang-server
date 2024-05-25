package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1Namespace;
import io.swagger.v3.oas.annotations.media.Schema;

public record NamespaceCategoryDto(
        @Schema(description = "클러스터 네임스페이스", example = "default")
        String namespace
) {
    public static NamespaceCategoryDto fromEntity(V1Namespace namespace) {
        return new NamespaceCategoryDto(
                namespace.getMetadata().getName());
    }
}
