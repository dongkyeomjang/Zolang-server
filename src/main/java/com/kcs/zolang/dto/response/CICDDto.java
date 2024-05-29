package com.kcs.zolang.dto.response;

import com.kcs.zolang.domain.Build;
import com.kcs.zolang.domain.CICD;
import lombok.Builder;

import java.time.LocalDateTime;
@Builder
public record CICDDto(
        Long repositoryId,
        String repositoryName,
        String lastCommit,
        String lastBuildStatus,
        LocalDateTime createdAt
) {
    public static CICDDto fromEntity(CICD cicd, Build lastBuild) {
        return CICDDto.builder()
                .repositoryId(cicd.getId())
                .repositoryName(cicd.getRepositoryName())
                .lastCommit(lastBuild.getLastCommitMessage())
                .lastBuildStatus(lastBuild.getBuildStatus())
                .createdAt(cicd.getCreatedAt())
                .build();
    }
}
