package com.kcs.zolang.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "커밋 생성 요청 Dto")
public record CommitDto(
        @JsonProperty("file_name") @Schema(description = "파일 이름", required = true)
        String fileName,
        @JsonProperty("content") @Schema(description = "커밋할 내용", required = true)
        String content,
        @JsonProperty("committer_name") @Schema(description = "커밋한 사람 이름", required = true)
        String committerName,
        @JsonProperty("committer_email") @Schema(description = "커밋한 사람 이메일", required = true)
        String committerEmail
) {
}