package com.kcs.zolang.dto.response.workload;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "PodListDto", description = "총 사용량과 Pod 목록 Dto")
public record PodListDto(
    List<UsageDto> totalUsage,
    List<PodSimpleDto> pods
) {

    public static PodListDto fromEntity(List<UsageDto> totalUsage, List<PodSimpleDto> pods) {
        return PodListDto.builder()
            .totalUsage(totalUsage)
            .pods(pods)
            .build();
    }
}
