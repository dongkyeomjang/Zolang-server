package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.dto.request.CommitDto;
import com.kcs.zolang.service.GithubService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/github")
public class GithubController {
    private final GithubService githubService;
    @GetMapping("")
    public ResponseDto<?> getRepositories(@UserId Long userId) {
        return ResponseDto.ok(githubService.getRepositories(userId));
    }
    @GetMapping("/branches")
    public ResponseDto<?> getBranches(@UserId Long userId, @RequestParam String repoName) {
        return ResponseDto.ok(githubService.getBranches(userId, repoName));
    }
    @PutMapping("/commits")
    public ResponseDto<?> createCommit(@UserId Long userId, @RequestParam String repoName, @RequestParam String branchName, @RequestBody CommitDto commitDto) {
        return ResponseDto.ok(githubService.createCommit(userId, repoName, branchName, commitDto));
    }
}
