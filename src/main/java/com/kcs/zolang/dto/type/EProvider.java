package com.kcs.zolang.dto.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EProvider {
    DEFAULT("DEFAULT"),
    KAKAO("KAKAO"),
    APPLE("APPLE"),
    GITHUB("GITHUB");

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
