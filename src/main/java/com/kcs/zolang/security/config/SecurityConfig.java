package com.kcs.zolang.security.config;

import com.kcs.zolang.constants.Constants;
import com.kcs.zolang.security.filter.GlobalLoggerFilter;
import com.kcs.zolang.security.filter.JwtAuthenticationFilter;
import com.kcs.zolang.security.filter.JwtExceptionFilter;
import com.kcs.zolang.security.handler.jwt.JwtAccessDeniedHandler;
import com.kcs.zolang.security.handler.jwt.JwtAuthEntryPoint;
import com.kcs.zolang.security.handler.login.Oauth2FailureHandler;
import com.kcs.zolang.security.handler.login.Oauth2SuccessHandler;
import com.kcs.zolang.security.handler.logout.CustomLogoutProcessHandler;
import com.kcs.zolang.security.handler.logout.CustomLogoutResultHandler;
import com.kcs.zolang.security.service.CustomOauth2UserDetailService;
import com.kcs.zolang.security.service.CustomUserDetailService;
import com.kcs.zolang.utility.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.CorsFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomLogoutProcessHandler customSignOutProcessHandler;
    private final CustomLogoutResultHandler customSignOutResultHandler;
    private final CorsFilter corsFilter;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final CustomUserDetailService customUserDetailService;
    private final JwtUtil jwtUtil;
    private final Oauth2SuccessHandler oauth2SuccessHandler;
    private final Oauth2FailureHandler oauth2FailureHandler;
    private final CustomOauth2UserDetailService customOauth2UserDetailService;

    @Bean
    protected SecurityFilterChain securityFilterChain(final HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable) // csrf 보호 비활성화
                .formLogin(AbstractHttpConfigurer::disable) // formLogin 기본 인증 방식 해제
                .httpBasic(AbstractHttpConfigurer::disable) // httpBasic 기본 인증 방식 해제
                .sessionManagement((sessionManagement) ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션 사용 안하고 상태가 없는 방식으로 인증 = JWT 사용
                )
                .authorizeHttpRequests(registry ->
                        registry
                                .requestMatchers(Constants.NO_NEED_AUTH_URLS.toArray(String[]::new)).permitAll()
                                .requestMatchers(Constants.USER_URLS.toArray(String[]::new)).hasRole("USER")
                                .anyRequest().authenticated()
                );

        httpSecurity
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler)
                        .userInfoEndpoint(it -> it.userService(customOauth2UserDetailService))
                )
                .logout(configurer ->
                        configurer
                                .logoutUrl("/api/v1/auth/logout")
                                .addLogoutHandler(customSignOutProcessHandler)
                                .logoutSuccessHandler(customSignOutResultHandler)
                );

        httpSecurity
                .exceptionHandling(configurer ->
                        configurer
                                .authenticationEntryPoint(jwtAuthEntryPoint)
                                .accessDeniedHandler(jwtAccessDeniedHandler)
                );
        httpSecurity
                .addFilterBefore(corsFilter, CorsFilter.class)
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtUtil, customUserDetailService),
                        LogoutFilter.class)
                .addFilterBefore(
                        new JwtExceptionFilter(),
                        JwtAuthenticationFilter.class)
                .addFilterBefore(
                        new GlobalLoggerFilter(),
                        JwtExceptionFilter.class);

        return httpSecurity.build();
    }
}
