package com.kcs.zolang.service;

import com.kcs.zolang.domain.User;
import com.kcs.zolang.dto.response.UserDetailDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    public boolean checkDuplicate(String nickname) {
        return userRepository.existsByNickname(nickname);
    }
}
