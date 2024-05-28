package com.kcs.zolang.dto.response;

public record BuildDto(
        String repositoryName,
        Long buildNumber,
        String lastCommitMessage,
        String buildStatus,
        String createdAt
) {
}