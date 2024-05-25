package com.kcs.zolang.dto.response.workload;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "JobListDto", description = "잡 리스트 Dto")
public record JobListDto(
    @Schema(description = "잡 리스트")
    List<JobSimpleDto> jobs,
    @Schema(description = "잡 수")
    int total,
    @Schema(description = "시작 인덱스")
    int start,
    @Schema(description = "끝 인덱스")
    int end,
    @Schema(description = "다음 페이지 토큰")
    String continueToken
) {

    public static JobListDto fromEntity(List<JobSimpleDto> jobs,
        String continueToken, int start, int total) {
        return JobListDto.builder()
            .jobs(jobs)
            .total(total)
            .start(start)
            .end(start + jobs.size() - 1)
            .continueToken(continueToken)
            .build();
    }
}
