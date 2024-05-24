package com.kcs.zolang.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClusterVersionRequestDto(
        @JsonProperty("domain_url")
        String domainUrl,
        @JsonProperty("secret_token")
        String secretToken
) {
}
