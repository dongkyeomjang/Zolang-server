package com.kcs.zolang.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CICDRequestDto(
        @JsonProperty("repo_name")
        String repoName,
        @JsonProperty("branch")
        String branch,
        @JsonProperty("language")
        String language,
        @JsonProperty("version")
        String version,
        @JsonProperty("build_tool")
        String buildTool,
        @JsonProperty("envVars")
        List<EnvVarDto> envVars,
        @JsonProperty("trigger")
        List<String> trigger
) {
}
