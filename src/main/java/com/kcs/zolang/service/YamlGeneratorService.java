package com.kcs.zolang.service;

import com.kcs.zolang.dto.response.GitRepoDto;
import com.kcs.zolang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class YamlGeneratorService {
    private final UserRepository userRepository;
    public List<GitRepoDto> getRepositories(Long userId) {
        RestTemplate restTemplate = new RestTemplate();
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid user ID"))
                        .getDomainUrl())
                .path("/repos");

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                builder.build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(null, null),
                (Class<List<Map<String, Object>>>)(Class<?>)List.class);

        List<Map<String, Object>> repos = response.getBody();
        return repos.stream()
                .map(repo -> GitRepoDto.builder()
                        .name(repo.get("name").toString())
                        .branchesUrl(repo.get("branches_url").toString())
                        .commitsUrl(repo.get("commits_url").toString())
                        .build())
                .collect(Collectors.toList());
    }
}
