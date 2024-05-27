package com.kcs.zolang.dto.response.workload;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "PodMetricsDto", description = "Pod 메트릭스 Dto")
public record PodMetricsDto(
    @Schema(description = "Pod 현재 자원 사용량")
    UsageDto usage,
    @Schema(description = "Pod 지난 자원 사용량 리스트")
    List<UsageDto> metrics
) {

    public static PodMetricsDto fromEntity(UsageDto usage, List<UsageDto> metrics) {
        return PodMetricsDto.builder()
            .usage(usage)
            .metrics(metrics)
            .build();
    }
}
