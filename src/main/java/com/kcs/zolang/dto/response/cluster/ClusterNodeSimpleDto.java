package com.kcs.zolang.dto.response.cluster;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;


@Builder
@Schema(name = "ClusterNodeSimpleDto", description = "유저 ClusterNodeList 정보 Dto")
public record ClusterNodeSimpleDto(

        @Schema(description = "name", example = "ip-172-31-11-72.ap-northeast-2.compute.internal")
        String name,

        //할당
        @Schema(description = "node cpu 할당", example = "1930m")
        String allocatableCpu,
        @Schema(description = "node memory 할당", example = "3446636Ki")
        String allocatableMemory,
        @Schema(description = "node pod 할당", example = "17")
        String allocatablePod,

        //전체
        @Schema(description = "node cpu capacity", example = "2")
        String capacityCpu,
        @Schema(description = "node memory capacity", example = "4001644Ki")
        String capacityMemory,
        @Schema(description = "node pod capacity", example = "17")
        String capacityPod,

        //사용량
        @Schema(description = "node cpu, memory usage", example = "")
        NodeUsageDto usage

) {
    public static ClusterNodeSimpleDto fromEntity(ClusterNodeListDto nodeDto) {
        return ClusterNodeSimpleDto.builder()
                .name(nodeDto.name())

                .allocatableCpu(nodeDto.allocatableCpu())
                .allocatableMemory(nodeDto.allocatableMemory())
                .allocatablePod(nodeDto.allocatablePod())

                .capacityCpu(nodeDto.capacityCpu())
                .capacityMemory(nodeDto.capacityMemory())
                .capacityPod(nodeDto.capacityPod())

                .usage(nodeDto.usage())
                .build();
    }
}
