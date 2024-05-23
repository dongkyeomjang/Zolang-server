package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Builder;

@Builder
@Schema(name = "CommonControllerResourceDto", description = "컨트롤러 리소스 Dto")
public record CommonControllerResourceDto(
    @Schema(description = "샐렉터", example = "app:nginx")
    Map<String, String> selector,
    @Schema(description = "이미지", example = "nginx:1.14.2")
    String image
) {

    public static CommonControllerResourceDto fromEntity(V1DaemonSet daemonSet) {
        return CommonControllerResourceDto.builder()
            .selector(daemonSet.getSpec().getSelector().getMatchLabels())
            .image(daemonSet.getSpec().getTemplate().getSpec().getContainers().get(0).getImage())
            .build();
    }

    public static CommonControllerResourceDto fromEntity(V1StatefulSet statefulSet) {
        return CommonControllerResourceDto.builder()
            .selector(statefulSet.getSpec().getSelector().getMatchLabels())
            .image(statefulSet.getSpec().getTemplate().getSpec().getContainers().get(0).getImage())
            .build();
    }

    public static CommonControllerResourceDto fromEntity(V1ReplicaSet replicaSet) {
        return CommonControllerResourceDto.builder()
            .selector(replicaSet.getSpec().getSelector().getMatchLabels())
            .image(replicaSet.getSpec().getTemplate().getSpec().getContainers().get(0).getImage())
            .build();
    }
}
