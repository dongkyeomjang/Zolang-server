package com.kcs.zolang.dto.response;

import static com.kcs.zolang.utility.MonitoringUtil.getAge;

import io.kubernetes.client.openapi.models.V1ServicePort;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import io.kubernetes.client.openapi.models.V1Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Builder
@Schema(name = "ServiceListDto", description = "유저 Servicelist 정보 Dto")
public record ServiceListDto(
        @Schema(description = "Network(service) 이름", example = "kubernetes")
        String serviceName,
        @Schema(description = "Network(service) 네임스페이스", example = "default")
        String serviceNamespace,
        @Schema(description = "Network(service) 레이블", example = "component : apiserver")
        Map<String, String> serviceLabels,
        @Schema(description = "Network(service) clusterIP 주소", example = "clusterIP:10.100.0.1")
        String serviceClusterIP,
        @Schema(description = "Network(service) externalIP 주소", example = "externalIP:10.100.0.1")
        List<String> serviceExternalIP,
        @Schema(description = "Network(service) port", example = "443")
        List<Integer> servicePort,
        @Schema(description = "Network(service) 생성 시간", example = "1d")
        String serviceAge
) {
    public static ServiceListDto fromEntity(V1Service service) {
        return ServiceListDto.builder()
                .serviceName(service.getMetadata().getName())
                .serviceNamespace(service.getMetadata().getNamespace())
                .serviceLabels(service.getMetadata().getLabels())
                .serviceClusterIP(service.getSpec().getClusterIP())
                .serviceExternalIP(service.getSpec().getExternalIPs())
                .servicePort(service.getSpec().getPorts().stream().map(V1ServicePort::getPort).toList())
                .serviceAge(getAge(Objects.requireNonNull(service.getMetadata().getCreationTimestamp()).toLocalDateTime()))
                .build();
    }
}
