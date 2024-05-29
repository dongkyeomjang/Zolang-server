package com.kcs.zolang.dto.response.cluster;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserUsageDto(
        //cpu
        @Schema(description = "모든 클러스터 전체 CPU 사용", example = "1.5")
        double cpuUsage,
        @Schema(description = "모든 클러스터 전체 CPU 할당", example = "2.0")
        double cpuAllocatable,
        @Schema(description = "모든 클러스터 전체 CPU 전체", example = "4.0")
        double cpuCapacity,

        //memory
        @Schema(description = "모든 클러스터 전체 Memory 사용", example = "2048")
        long memoryUsage,
        @Schema(description = "모든 클러스터 전체 Memory 할당", example = "2.0")
        long memoryAllocatable,
        @Schema(description = "모든 클러스터 전체 Memory 전체", example = "4.0")
        long memoryCapacity,

        //pod
        @Schema(description = "모든 클러스터 전체 Pod 사용", example ="5")
        int podUsage,
        @Schema(description = "모든 클러스터 전체 Pod 할당", example = "2.0")
        int podAllocatable,
        @Schema(description = "모든 클러스터 전체 Pod 전체", example = "4.0")
        int podCapacity

) {
}
