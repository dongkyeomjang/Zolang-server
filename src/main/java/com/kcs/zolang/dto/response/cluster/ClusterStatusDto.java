package com.kcs.zolang.dto.response.cluster;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;


@Builder
@Schema(name = "ClusterStatusDto", description = "클러스터 전체 상태 정보 Dto")
public record ClusterStatusDto(
        //cpu
        @Schema(description = "클러스터 전체 CPU 사용", example = "1.5")
        double cpuUsage,
        @Schema(description = "클러스터 전체 CPU 할당", example = "2.0")
        double cpuAllocatable,
        @Schema(description = "클러스터 전체 CPU 전체", example = "4.0")
        double cpuCapacity,

        //cluster
        @Schema(description = "클러스터 전체 메모리 사용", example = "2048")
        long memoryUsage,
        @Schema(description = "클러스터 전체 메모리 할당", example = "4096")
        long memoryAllocatable,
        @Schema(description = "클러스터 전체 메모리 전체", example = "8192")
        long memoryCapacity,

        //pod
        @Schema(description = "클러스터 전체 파드 사용", example ="5")
        int podUsage,
        @Schema(description = "클러스터 전체 파드 할당", example = "50")
        int podAllocatable,
        @Schema(description = "클러스터 전체 파드 전체", example = "100")
        int podCapacity

) {
}
