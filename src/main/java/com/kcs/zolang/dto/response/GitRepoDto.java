package com.kcs.zolang.dto.response;

import lombok.Builder;

@Builder
public record GitRepoDto(
        String name,
        String branchesUrl
) {
}
