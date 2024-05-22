package com.kcs.zolang.security.handler.login;

import com.kcs.zolang.dto.response.JwtTokenDto;
import com.kcs.zolang.repository.UserRepository;
import com.kcs.zolang.security.info.AuthenticationResponse;
import com.kcs.zolang.security.info.UserPrincipal;
import com.kcs.zolang.utility.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
@Component
@RequiredArgsConstructor
@Slf4j
public class Oauth2SuccessHandler implements AuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final StringEncryptor stringEncryptor; // StringEncryptor 추가

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName());
        log.info("Oauth2SuccessHandler 진입 성공. client = {}", client);

        if (client == null) {
            throw new IllegalArgumentException("No client information available in session");
        }

        String originalAccessToken = client.getAccessToken().getTokenValue();
        String encryptedAccessToken = stringEncryptor.encrypt(originalAccessToken); // 액세스 토큰 암호화
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        JwtTokenDto jwtTokenDto = jwtUtil.generateTokens(principal.getUserId(), principal.getRole());
        log.info("user 정보: {}", principal.getUserId());
        userRepository.updateRefreshTokenAndLoginStatusAndGithubAccessToken(
                principal.getUserId(), jwtTokenDto.refreshToken(), true, encryptedAccessToken); // 암호화된 토큰 저장

        AuthenticationResponse.makeLoginSuccessResponse(response, jwtTokenDto, jwtUtil.getRefreshExpiration());
        response.sendRedirect("https://www.zolang.site");
    }
}
