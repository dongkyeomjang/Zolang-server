package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.constants.Constants;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.dto.request.AuthSignUpDto;
import com.kcs.zolang.dto.request.OauthSignUpDto;
import com.kcs.zolang.dto.response.JwtTokenDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.service.AuthService;
import com.kcs.zolang.utility.CookieUtil;
import com.kcs.zolang.utility.HeaderUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/auth/sign-up")
    public ResponseDto<?> signUp(
            @RequestBody @Valid AuthSignUpDto authSignUpDto
    ) {
        authService.signUp(authSignUpDto);

        return ResponseDto.ok(null);
    }
    @PostMapping("/oauth2/sign-up")
    public ResponseDto<?> signUp(@UserId Long userId, @RequestBody OauthSignUpDto oauthSignUpDto){
        authService.signUp(userId, oauthSignUpDto);
        return ResponseDto.ok(null);
    }

    @PostMapping("/auth/reissue")
    public ResponseDto<?> reissue(
            HttpServletRequest request,
            HttpServletResponse response,
            @UserId Long userId) {
        String refreshToken = StringUtils.startsWith(request.getHeader("User-Agent"), "Dart") ?
                HeaderUtil.refineHeader(request, Constants.AUTHORIZATION_HEADER, Constants.BEARER_PREFIX)
                        .orElseThrow(() -> new CommonException(ErrorCode.MISSING_REQUEST_HEADER))
                : CookieUtil.refineCookie(request, "refresh_token")
                .orElseThrow(() -> new CommonException(ErrorCode.MISSING_REQUEST_HEADER));

        JwtTokenDto jwtTokenDto = authService.reissue(userId, refreshToken);

        if (request.getHeader("User-Agent") != null) {
            CookieUtil.addSecureCookie(response, "refresh_token", jwtTokenDto.refreshToken(), 60 * 60 * 24 * 14);
            jwtTokenDto = JwtTokenDto.of(jwtTokenDto.accessToken(), null);
        }

        return ResponseDto.ok(jwtTokenDto);
    }

}
