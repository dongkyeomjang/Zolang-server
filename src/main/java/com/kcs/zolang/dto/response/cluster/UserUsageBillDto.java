package com.kcs.zolang.dto.response.cluster;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "UserUsageBillDto", description = "사용자 사용량 청구 정보 Dto")
public record UserUsageBillDto(
        String date,
        @Schema(description = "총 CPU 사용량", example = "10.5")
        double totalCpuUsage,
        @Schema(description = "총 CPU 비용", example = "100.5")
        double totalCpuCost,
        @Schema(description = "총 Memory 사용량", example = "20480")
        long totalMemoryUsage,
        @Schema(description = "총 Memory 비용", example = "100.5")
        double totalMemoryCost,
        @Schema(description = "총 Pod 사용량", example = "50")
        int totalPodUsage,
        @Schema(description = "총 Pod 비용", example = "100.5")
        double totalPodCost,
        @Schema(description = "총 클러스터 런타임 (분)", example = "6000")
        long totalClusterRuntime,
        @Schema(description = "총 클러스터 런타임 (분)", example = "6000")
        double totalClusterRuntimeCost,
        @Schema(description = "총 비용", example = "1200.5")
        double totalCost
) {
}
