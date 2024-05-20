package com.kcs.zolang.utility;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

import java.util.Arrays;
import java.util.Optional;

public class CookieUtil {
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return Optional.empty();
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return Optional.of(cookie);
            }
        }

        return Optional.empty();
    }

    public static void addCookie(HttpServletResponse response, String name, String value) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .domain(".zolang.site") // 도메인 설정, 필요에 따라 생략 가능
                .maxAge(60 * 60 * 24 * 30) // 30일 설정
                .httpOnly(true) // HttpOnly 설정
                .sameSite("Lax") // 크로스 사이트 요청 허용
                .secure(true) // HTTPS를 통해서만 쿠키 전송
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void addSecureCookie(HttpServletResponse response, String name, String value, Integer maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .domain(".zolang.site") // 도메인 설정, 필요에 따라 생략 가능
                .maxAge(maxAge)
                .httpOnly(true) // HttpOnly 설정
                .sameSite("Lax") // 크로스 사이트 요청 허용
                .secure(true) // HTTPS를 통해서만 쿠키 전송
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                ResponseCookie deleteCookie = ResponseCookie.from(name,null)
                        .path("/")
                        .domain(".zolang.site") // 도메인 설정, 필요에 따라 생략 가능
                        .maxAge(0)
                        .httpOnly(true) // HttpOnly 설정
                        .sameSite("Lax") // 크로스 사이트 요청 허용
                        .secure(true) // HTTPS를 통해서만 쿠키 전송
                        .build();
                response.addHeader("Set-Cookie", deleteCookie.toString());
            }
        }
    }
    public static Optional<String> refineCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(name))
                .findFirst().map(Cookie::getValue);
    }
}
