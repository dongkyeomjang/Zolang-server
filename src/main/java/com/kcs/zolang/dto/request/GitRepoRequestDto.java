package com.kcs.zolang.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitRepoDto(
        @JsonProperty("name")
        String name,
        @JsonProperty("branches_url")
        String branchesUrl,
        @JsonProperty("commits_url")
        String commitsUrl
) {
}
