package com.kcs.zolang.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OauthSignUpDto(
        @JsonProperty("nickname")
        String nickname
) {
}