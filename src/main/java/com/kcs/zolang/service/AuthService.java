package com.kcs.zolang.service;

import com.kcs.zolang.domain.User;
import com.kcs.zolang.dto.request.AuthSignUpDto;
import com.kcs.zolang.dto.request.OauthSignUpDto;
import com.kcs.zolang.dto.response.JwtTokenDto;
import com.kcs.zolang.dto.type.ERole;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.UserRepository;
import com.kcs.zolang.utility.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public void signUp(AuthSignUpDto authSignUpDto) {
        userRepository.save(
                User.signUp(authSignUpDto, bCryptPasswordEncoder.encode(authSignUpDto.password()))
        );
    }
    @Transactional
    public void signUp(Long userId, OauthSignUpDto oauthSignUpDto){
        User oauthUser = userRepository.findById(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER));

        oauthUser.register(oauthSignUpDto.nickname());
    }
    @Transactional
    public JwtTokenDto reissue(Long userId, String refreshToken) {
        User user = userRepository.findByIdAndRefreshTokenAndIsLogin(userId, refreshToken, true)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_LOGIN_USER));

        JwtTokenDto jwtTokenDto = jwtUtil.generateTokens(user.getId(), user.getRole());
        user.updateRefreshToken(jwtTokenDto.refreshToken());

        return jwtTokenDto;
    }
}
