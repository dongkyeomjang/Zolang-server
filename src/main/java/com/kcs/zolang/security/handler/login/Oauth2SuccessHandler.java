package com.kcs.zolang.security.handler.login;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
        log.info("originalAccessToken: {}", originalAccessToken);
        String encryptedAccessToken = stringEncryptor.encrypt(originalAccessToken); // 액세스 토큰 암호화
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        JwtTokenDto jwtTokenDto = jwtUtil.generateTokens(principal.getUserId(), principal.getRole());
        log.info("user 정보: {}", principal.getUserId());
        userRepository.updateRefreshTokenAndLoginStatusAndGithubAccessToken(
                principal.getUserId(), jwtTokenDto.refreshToken(), true, encryptedAccessToken); // 암호화된 토큰 저장

        String url = "https://api.github.com/user/installations";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + originalAccessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        boolean isAppInstalled = false;
        if (res.getBody() != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseBody = objectMapper.readValue(res.getBody(), Map.class);
            List<Map<String, Object>> installations = (List<Map<String, Object>>) responseBody.get("installations");
            for (Map<String, Object> installation : installations) {
                Map<String, Object> app = (Map<String, Object>) installation.get("app");
                if ("zolang-app".equals(installation.get("app_slug"))) {
                    isAppInstalled = true;
                    break;
                }
            }
        }

        if (!isAppInstalled) {
            // GitHub 앱이 설치되지 않은 경우
            String installUrl = "https://github.com/apps/zolang-app/installations/new";
            response.sendRedirect(installUrl);
        } else {
            AuthenticationResponse.makeLoginSuccessResponse(response, jwtTokenDto, jwtUtil.getRefreshExpiration());
            response.sendRedirect("https://www.zolang.store");
        }
    }
}
