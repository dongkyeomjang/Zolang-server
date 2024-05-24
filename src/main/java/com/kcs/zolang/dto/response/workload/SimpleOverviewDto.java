package com.kcs.zolang.dto.response.workload;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Builder;

@Builder
public record SimpleOverviewDto(
    @Schema(description = "전체 개수")
    int counts,
    @Schema(description = "실행 중인 개수")
    int running
) implements Serializable {

    public static SimpleOverviewDto of(int counts, int podRunning) {
        return SimpleOverviewDto.builder()
            .counts(counts)
            .running(podRunning)
            .build();
    }
}
