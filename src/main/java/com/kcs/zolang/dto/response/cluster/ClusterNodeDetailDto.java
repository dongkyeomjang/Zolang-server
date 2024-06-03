package com.kcs.zolang.dto.response.cluster;

import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kcs.zolang.utility.MonitoringUtil.getAge;

@Builder
@Schema(name = "ClusterNodeDetailDto", description = "특정 노드의 상세 정보 Dto")
public record ClusterNodeDetailDto(
        @Schema(description = "특정 노드의 allocatable 데이터", example = "")
        Map<String, String> allocatable,
        @Schema(description = "특정 노드의 OsImage", example = "aws-eks-...")
        String osImage,
        @Schema(description = "특정 노드의 addresses", example = "")
        Map<String, String> addresses,
        @Schema(description = "Kubectl 버전", example = "v1.29.3-eks-ae9a62a")
        String kubectlVersion,
        @Schema(description = "특정 노드의 OS", example = "Linux")
        String os,
        @Schema(description = "특정 노드 컨테이너 런타임", example = "docker://25.0.3")
        String containerRuntime,
        @Schema(description = "특정 노드 생성 시간", example = "2024-03-20T14:26:36Z")
        String created,
        @Schema(description = "특정 노드 커널버전", example = "6.6.12-linuxkit")
        String kernelVersion,
        @Schema(description = "특정 노드 이름", example = "ip-172-31-11-72.ap-northeast-2.compute.internal")
        String name,
        @Schema(description = "특정 노드의 conditions 데이터", example = "")
        List<NodeConditionDto> conditions,
        @Schema(description = "특정 노드의 capacity 데이터", example = "")
        Map<String, String> capacity
) {
    public static ClusterNodeDetailDto fromEntity(V1Node node) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        OffsetDateTime creationTimestamp = node.getMetadata().getCreationTimestamp();

        return ClusterNodeDetailDto.builder()
                .allocatable(node.getStatus().getAllocatable().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getNumber().toString())))
                .osImage(node.getStatus().getNodeInfo().getOsImage())
                .addresses(node.getStatus().getAddresses().stream()
                        .collect(Collectors.toMap(address -> address.getType(), address -> address.getAddress())))
                .kubectlVersion(node.getStatus().getNodeInfo().getKubeletVersion())
                .os(node.getStatus().getNodeInfo().getOperatingSystem())
                .containerRuntime(node.getStatus().getNodeInfo().getContainerRuntimeVersion())
                .created(creationTimestamp != null ? formatter.format(creationTimestamp) : "N/A")
                .kernelVersion(node.getStatus().getNodeInfo().getKernelVersion())
                .name(node.getMetadata().getName())
                .conditions(node.getStatus().getConditions().stream().map(NodeConditionDto::fromEntity).collect(Collectors.toList()))
                .capacity(node.getStatus().getCapacity().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getNumber().toString())))
                .build();
    }
}
