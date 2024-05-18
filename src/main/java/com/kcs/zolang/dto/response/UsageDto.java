package com.kcs.zolang.dto.response;

import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.custom.Quantity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Map;
import lombok.Builder;

@Builder
@Schema(name = "UsageDto", description = "Pod 사용량 Dto")
public record UsageDto(
    @Schema(description = "시간", example = "00:23")
    String time,
    @Schema(description = "Pod cpu 사용량", example = "1")
    double cpuUsage,
    @Schema(description = "Pod 메모리 사용량", example = "8090432")
    long memoryUsage
) {

    public static UsageDto fromEntity(PodMetrics podMetrics, String time) {
        Map<String, Quantity> usage = podMetrics.getContainers().get(0).getUsage();
        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.UP);
        double formattedCpuUsage = Double.parseDouble(
            df.format(usage.get("cpu").getNumber().doubleValue()));
        return UsageDto.builder()
            .time(time)
            .cpuUsage(formattedCpuUsage)
            .memoryUsage(usage.get("memory").getNumber().longValue())
            .build();
    }

    public static UsageDto fromEntity(double cpuUsage, long memoryUsage, String time) {
        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return UsageDto.builder()
            .time(time)
            .cpuUsage(Double.parseDouble(df.format(cpuUsage)))
            .memoryUsage(memoryUsage)
            .build();
    }

    @Override
    public String toString() {
        return "UserDTO{" +
            "cpuUsage=" + cpuUsage + '\'' +
            ", memoryUsage='" + memoryUsage + '\'' +
            '}';
    }
}
