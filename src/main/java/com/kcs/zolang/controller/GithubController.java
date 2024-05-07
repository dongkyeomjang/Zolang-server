package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.service.YamlGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/yaml-generator")
public class YamlGeneratorController {
    private final YamlGeneratorService yamlGeneratorService;
    @GetMapping("/repositories")
    public ResponseDto<?> getRepositories(@UserId Long userId) {
        return ResponseDto.ok(yamlGeneratorService.getRepositories(userId));
    }
}
