package com.kcs.zolang.dto.response;

import com.kcs.zolang.domain.Build;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BuildDto(
        Integer buildNumber,
        String lastCommitMessage,
        String buildStatus,
        LocalDateTime createdAt
) {
    public static BuildDto fromEntity(Build build){
        return BuildDto.builder()
                .buildNumber(build.getBuildNumber())
                .lastCommitMessage(build.getLastCommitMessage())
                .buildStatus(build.getBuildStatus())
                .createdAt(build.getCreatedAt())
                .build();
    }
}