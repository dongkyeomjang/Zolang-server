package com.kcs.zolang.controller;

import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.service.CICDService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhook")
public class WebhookController {
    private final CICDService cicdService;

    @PostMapping("")
    public ResponseDto<?> handleGithubWebhook(@RequestBody Map<String, Object> payload) {
        cicdService.handleGithubWebhook(payload);
        return ResponseDto.ok(null);
    }
}
