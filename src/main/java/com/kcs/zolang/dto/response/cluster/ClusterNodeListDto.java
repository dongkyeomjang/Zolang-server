package com.kcs.zolang.dto.response.cluster;

import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Builder
@Schema(name = "ClusterNodeListDto", description = "유저 ClusterNodeList 정보 Dto")
public record ClusterNodeListDto(

        //metadata
        @Schema(description = "name", example = "ip-172-31-11-72.ap-northeast-2.compute.internal")
        String name,
        @Schema(description = "Kubectl 버전", example = "v1.29.3-eks-ae9a62a")
        String kubectlVersion,
        @Schema(description = "creation Timestamp", example = "2024-05-10T06:58:44Z")
        String timeStamp,

        //status-allocatable
        @Schema(description = "node cpu 할당", example = "1930m")
        String allocatableCpu,
        @Schema(description = "node memory 할당", example = "3446636Ki")
        String allocatableMemory,
        @Schema(description = "node pod 할당", example = "17")
        String allocatablePod,

        //status-capacity
        @Schema(description = "node cpu capacity", example = "2")
        String capacityCpu,
        @Schema(description = "node memory capacity", example = "4001644Ki")
        String capacityMemory,
        @Schema(description = "node pod capacity", example = "17")
        String capacityPod,

        //status-condition
        @Schema(description = "type에 따른 노드 conditions", example = "")
        List<V1NodeCondition> conditions,

        //status-usage
        @Schema(description = "node cpu, memory usage", example = "")
        NodeUsageDto usage

) {
    public static ClusterNodeListDto fromEntity(V1Node node, NodeUsageDto usage) {
        return ClusterNodeListDto.builder()
                .name(node.getMetadata().getName())
                .kubectlVersion(node.getStatus().getNodeInfo().getKubeletVersion())
                .timeStamp(node.getMetadata().getCreationTimestamp().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .allocatableCpu(node.getStatus().getAllocatable().get("cpu").getNumber().toString())
                .allocatableMemory(node.getStatus().getAllocatable().get("memory").getNumber().toString())
                .allocatablePod(node.getStatus().getAllocatable().get("pods").getNumber().toString())
                .capacityCpu(node.getStatus().getCapacity().get("cpu").getNumber().toString())
                .capacityMemory(node.getStatus().getCapacity().get("memory").getNumber().toString())
                .capacityPod(node.getStatus().getCapacity().get("pods").getNumber().toString())
                .conditions(node.getStatus().getConditions())
                .usage(usage)
                .build();
    }
}
