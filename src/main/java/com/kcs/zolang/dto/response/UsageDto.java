package com.kcs.zolang.dto.response;

import static com.kcs.zolang.utility.MonitoringUtil.byteConverter;

import io.kubernetes.client.custom.PodMetrics;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "UsageDto", description = "Pod 사용량 Dto")
public record UsageDto(
    @Schema(description = "Pod cpu 사용량", example = "1.00m")
    String cpuUsage,
    @Schema(description = "Pod 메모리 사용량", example = "80.90Mi")
    String memoryUsage
) {

    public static UsageDto fromEntity(PodMetrics podMetrics) {
        return UsageDto.builder()
            .cpuUsage(podMetrics.getContainers().stream()
                .map(it -> it.getUsage().get("cpu").getNumber().doubleValue()) + "m")
            .memoryUsage(podMetrics.getContainers().stream().map(it -> byteConverter(
                String.valueOf(it.getUsage().get("memory").getNumber().doubleValue()))).toString())
            .build();
    }
}
