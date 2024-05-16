package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.dto.request.GitRepoRequestDto;
import com.kcs.zolang.service.CICDService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cicd")
public class CICDController {
    private final CICDService cicdService;

    @PostMapping("/register")
    public ResponseDto<?> registerRepository(@UserId Long userId, GitRepoRequestDto requestDto) {
        cicdService.registerRepository(userId, requestDto);
        return ResponseDto.created(null);
    }
}
