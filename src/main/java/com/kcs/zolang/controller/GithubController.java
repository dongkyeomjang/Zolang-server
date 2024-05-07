package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.service.GithubService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/github-repo")
public class GithubController {
    private final GithubService yamlGeneratorService;
    @GetMapping("")
    public ResponseDto<?> getRepositories(@UserId Long userId) {
        return ResponseDto.ok(yamlGeneratorService.getRepositories(userId));
    }
    @GetMapping("/{repoName}/branches")
    public ResponseDto<?> getBranches(@UserId Long userId, @PathVariable String repoName) {
        return ResponseDto.ok(yamlGeneratorService.getBranches(userId, repoName));
    }
}
