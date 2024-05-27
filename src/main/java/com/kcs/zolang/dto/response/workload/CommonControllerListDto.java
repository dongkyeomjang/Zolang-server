package com.kcs.zolang.dto.response.workload;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "CommonControllerListDto", description = "컨트롤러 목록 Dto")
public record CommonControllerListDto(
    @Schema(description = "컨트롤러 목록")
    List<CommonControllerDto> controllers,
    @Schema(description = "총 컨트롤러 수")
    int total,
    @Schema(description = "시작 인덱스")
    int start,
    @Schema(description = "끝 인덱스")
    int end,
    @Schema(description = "다음 페이지 토큰")
    String continueToken
) {

    public static CommonControllerListDto fromEntity(List<CommonControllerDto> controllers,
        String continueToken, int start, int total) {
        return CommonControllerListDto.builder()
            .controllers(controllers)
            .total(total)
            .start(start)
            .end(start + controllers.size() - 1)
            .continueToken(continueToken)
            .build();
    }
}
