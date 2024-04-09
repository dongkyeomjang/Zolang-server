package com.kcs.zolang.security.info;

import com.kcs.zolang.dto.type.ERole;

public record JwtUserInfo(Long userId, ERole role) {
}
