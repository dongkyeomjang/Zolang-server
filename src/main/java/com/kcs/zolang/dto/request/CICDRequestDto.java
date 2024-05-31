package com.kcs.zolang.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

public record GitRepoRequestDto(
        @JsonProperty("repo_name")
        String repoName
) {
}
