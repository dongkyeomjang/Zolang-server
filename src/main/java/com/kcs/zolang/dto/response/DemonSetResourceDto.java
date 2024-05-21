package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Builder;

@Builder
@Schema(name = "DemonSetResourceDto", description = "데몬셋 리소스 Dto")
public record DemonSetResourceDto(
    @Schema(description = "샐렉터", example = "app:nginx")
    Map<String, String> selector,
    @Schema(description = "이미지", example = "nginx:1.14.2")
    String image
) {

    public static DemonSetResourceDto fromEntity(V1DaemonSet daemonSet) {
        return DemonSetResourceDto.builder()
            .selector(daemonSet.getSpec().getSelector().getMatchLabels())
            .image(daemonSet.getSpec().getTemplate().getSpec().getContainers().get(0).getImage())
            .build();
    }
}
