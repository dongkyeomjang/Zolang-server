package com.kcs.zolang.dto.response.workload;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "PodListDto", description = "총 사용량과 Pod 목록 Dto")
public record PodListDto(
    List<UsageDto> totalUsage,
    List<PodSimpleDto> pods,
    int total,
    int start,
    int end,
    String continueToken
) {

    public static PodListDto fromEntity(List<UsageDto> totalUsage, List<PodSimpleDto> pods,
        String continueToken, int start, int total) {
        return PodListDto.builder()
            .totalUsage(totalUsage)
            .pods(pods)
            .total(total)
            .start(start)
            .end(start + pods.size() - 1)
            .continueToken(continueToken)
            .build();
    }
}
