package com.kcs.zolang.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EnvVarDto(
        @JsonProperty("key")
        String key,
        @JsonProperty("value")
        String value
) {}