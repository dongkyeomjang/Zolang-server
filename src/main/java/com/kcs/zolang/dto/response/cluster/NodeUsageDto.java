package com.kcs.zolang.dto.response.cluster;

import io.kubernetes.client.custom.NodeMetrics;
import io.kubernetes.client.custom.Quantity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Map;
import lombok.Builder;

@Builder
@Schema(name = "NodeUsageDto", description = "Node 사용량 Dto")
public record NodeUsageDto(
        @Schema(description = "시간", example = "00:23")
        String time,
        @Schema(description = "Node cpu 사용량", example = "1")
        double nodeCpuUsage,
        @Schema(description = "Node 메모리 사용량", example = "8090432")
        long nodeMemoryUsage
) {

    public static NodeUsageDto fromEntity(NodeMetrics nodeMetrics, String time) {
        Map<String, Quantity> usage = nodeMetrics.getUsage();
        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.UP);
        double formattedCpuUsage = Double.parseDouble(
                df.format(usage.get("cpu").getNumber().doubleValue()));
        return NodeUsageDto.builder()
                .time(time)
                .nodeCpuUsage(formattedCpuUsage)
                .nodeMemoryUsage(usage.get("memory").getNumber().longValue())
                .build();
    }

    public static NodeUsageDto fromEntity(double nodeCpuUsage, long nodeMemoryUsage, String time) {
        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return NodeUsageDto.builder()
                .time(time)
                .nodeCpuUsage(Double.parseDouble(df.format(nodeCpuUsage)))
                .nodeMemoryUsage(nodeMemoryUsage)
                .build();
    }

    @Override
    public String toString() {
        return "NodeUsageDto{" +
                "nodeCpuUsage=" + nodeCpuUsage +
                ", nodeMemoryUsage=" + nodeMemoryUsage +
                '}';
    }
}
