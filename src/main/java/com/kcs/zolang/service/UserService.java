package com.kcs.zolang.service;

import com.kcs.zolang.domain.User;
import com.kcs.zolang.dto.response.UserDetailDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserDetailDto getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER));
        return UserDetailDto.fromEntity(user);
    }
    public UserDetailDto updateEmail(Long userId, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER));
        user.updateEmail(email);
        userRepository.save(user);
        return UserDetailDto.fromEntity(user);
    }
}
