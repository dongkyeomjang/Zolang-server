package com.kcs.zolang.dto.response;

import java.io.Serializable;
import lombok.Builder;

@Builder
public record SimpleOverviewDto(int counts, int running) implements Serializable {
    public static SimpleOverviewDto of(int counts, int podRunning){
        return SimpleOverviewDto.builder().counts(counts).running(podRunning).build();
    }
}
