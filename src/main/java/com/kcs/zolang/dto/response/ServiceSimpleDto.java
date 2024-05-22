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
    @Schema(description = "서비스 이름", example = "nginx")
    String name,
    @Schema(description = "서비스 네임스페이스", example = "default")
    String namespace,
    @Schema(description = "레이블 목록")
    Map<String, String> labels,
    @Schema(description = "서비스 타입", example = "ClusterIP")
    String type,
    @Schema(description = "클러스터 IP")
    String clusterIP,
    @Schema(description = "내부 엔드포인트 목록", example = "nginx.default:80 TCP")
    List<String> internalEndpoints,
    @Schema(description = "외부 엔드포인트 목록")
    List<String> externalEndpoints,
    @Schema(description = "생성 일시", example = "2024. 01. 01. AM 10:00")
    String creationDateTime,
    @Schema(description = "생성 시간", example = "1d")
    String age
) {

    public static ServiceSimpleDto fromEntity(V1Service service) {
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
