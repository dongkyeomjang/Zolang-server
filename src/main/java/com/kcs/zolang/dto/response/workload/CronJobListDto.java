package com.kcs.zolang.dto.response.workload;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "CronJobListDto", description = "크론잡 리스트 Dto")
public record CronJobListDto(
    List<ControllerCronJobDto> cronJobs,
    @Schema(description = "크론잡 수")
    int total,
    @Schema(description = "시작 인덱스")
    int start,
    @Schema(description = "끝 인덱스")
    int end,
    @Schema(description = "다음 페이지 토큰")
    String continueToken
) {

    public static CronJobListDto fromEntity(List<ControllerCronJobDto> cronJobs,
        String continueToken, int start, int total) {
        return CronJobListDto.builder()
            .cronJobs(cronJobs)
            .total(total)
            .start(start)
            .end(start + cronJobs.size() - 1)
            .continueToken(continueToken)
            .build();
    }
}
