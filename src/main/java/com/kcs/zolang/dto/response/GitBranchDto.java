package com.kcs.zolang.dto.response;

import lombok.Builder;

@Builder
public record GitBranchDto(
        String name,
        String commitsUrl,
        String commitSha,
        String isProtected
) {
}