package com.kcs.zolang.service;

import com.kcs.zolang.dto.response.GitBranchDto;
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
public class GithubService {
    private final UserRepository userRepository;
    public List<GitRepoDto> getRepositories(Long userId) {
        RestTemplate restTemplate = new RestTemplate();
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://api.github.com")
                .path("/users/"
                        + userRepository.findById(userId).get().getNickname()
                        + "/repos");

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
    public List<GitBranchDto> getBranches(Long userId, String repoName){
        RestTemplate restTemplate = new RestTemplate();
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://api.github.com")
                .path("/repos/"
                        + userRepository.findById(userId).get().getNickname()
                        + "/"
                        + repoName
                        + "/branches");
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                builder.build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(null, null),
                (Class<List<Map<String, Object>>>)(Class<?>)List.class);

        List<Map<String, Object>> branches = response.getBody();
        return branches.stream()
                .map(branch -> {
                    Map<String, Object> commit = (Map<String, Object>) branch.get("commit");
                    return GitBranchDto.builder()
                            .name(branch.get("name").toString())
                            .commitSha(commit.get("sha").toString())
                            .commitsUrl(commit.get("url").toString())
                            .isProtected(branch.get("protected").toString())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
