package com.kcs.zolang.dto.response;

import static com.kcs.zolang.utility.MonitoringUtil.DATE_TIME_FORMATTER;
import static com.kcs.zolang.utility.MonitoringUtil.getAge;

import io.kubernetes.client.openapi.models.V1Service;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;

@Builder
@Schema(name = "ServiceSimpleDto", description = "서비스 간단 Dto")
public record ServiceSimpleDto(
    String name,
    String namespace,
    Map<String, String> labels,
    String type,
    String clusterIP,
    List<String> internalEndpoints,
    List<String> externalEndpoints,
    String creationDateTime,
    String age
) {

    public static ServiceSimpleDto fromEntity(V1Service service) {
        String name = Objects.requireNonNull(service.getMetadata()).getName();
        String namespace = service.getMetadata().getNamespace();
        List<String> internalEndpoints = new java.util.ArrayList<>(
            service.getSpec().getPorts().stream()
                .map(
                    port -> service.getMetadata().getName() + "." + service.getMetadata()
                        .getNamespace()
                        + ":" + port.getPort() + " " + port.getProtocol()).toList());
        internalEndpoints.addAll(service.getSpec().getPorts().stream()
            .map(
                port -> service.getMetadata().getName() + "." + service.getMetadata().getNamespace()
                    + ":" + (port.getNodePort() == null ? 0 : port.getNodePort()) + " "
                    + port.getProtocol()).toList());
        List<String> externalEndpoints = service.getSpec().getPorts().stream()
            .flatMap(port -> service.getSpec().getExternalIPs() == null ? null
                : service.getSpec().getExternalIPs().stream()
                    .map(ip -> ip + ":" + port.getPort() + " " + port.getProtocol()))
            .toList();
        return ServiceSimpleDto.builder()
            .name(Objects.requireNonNull(service.getMetadata()).getName())
            .namespace(service.getMetadata().getNamespace())
            .labels(service.getMetadata().getLabels())
            .type(service.getSpec().getType())
            .clusterIP(service.getSpec().getClusterIP())
            .internalEndpoints(internalEndpoints)
            .externalEndpoints(externalEndpoints)
            .creationDateTime(
                Objects.requireNonNull(service.getMetadata().getCreationTimestamp())
                    .toLocalDateTime()
                    .format(DATE_TIME_FORMATTER))
            .age(getAge(
                service.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .build();
    }
}
