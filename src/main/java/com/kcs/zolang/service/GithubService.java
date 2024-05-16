package com.kcs.zolang.service;

import com.kcs.zolang.dto.request.CommitDto;
import com.kcs.zolang.dto.response.GitBranchDto;
import com.kcs.zolang.dto.response.GitRepoDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;
@Slf4j
@RequiredArgsConstructor
@Service
public class GithubService {
    private final UserRepository userRepository;
    private final StringEncryptor stringEncryptor;
    private final RestTemplate restTemplate;
    @Value("${github.app-id}")
    private String appId;
    @Value("${github.secret}")
    private String privateKeyStr;
    @Value("${github.webhook-url}")
    private String webhookUrl;

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + token);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        return headers;
    }

    public List<GitRepoDto> getRepositories(Long userId) {
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
                        .build())
                .collect(Collectors.toList());
    }
    public List<GitBranchDto> getBranches(Long userId, String repoName){
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
    public Boolean createCommit(Long userId, String repoName, String branchName, CommitDto commitDto) {
        String nickname = userRepository.findById(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER))
                .getNickname();

        String token = stringEncryptor.decrypt(userRepository.findById(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER))
                .getGithubAccessToken());
        log.info("token: {}", token);

        String path = String.format("/repos/%s/%s/contents/%s", nickname, repoName, commitDto.fileName());
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://api.github.com")
                .path(path)
                .queryParam("ref", branchName);

        HttpEntity<String> requestEntity = new HttpEntity<>(createHeaders(token));

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(
                    builder.build().encode().toUri(),
                    HttpMethod.GET,
                    requestEntity,
                    Map.class);
        } catch (HttpClientErrorException.NotFound e) { // 파일이 없다면 새로 생성
            return createFile(nickname, repoName, branchName, commitDto, token);
        }

        String sha = (String) response.getBody().get("sha");
        String existingContent = (String) response.getBody().get("content");
        String encodedContent = Base64.getEncoder().encodeToString(commitDto.content().getBytes());

        if (!encodedContent.equals(existingContent)) {
            return updateFile(nickname, repoName, branchName, commitDto, encodedContent, sha, token);
        } else {
            System.out.println("Nothing to update");
            return false;
        }
    }

    private Boolean createFile(String nickname, String repoName, String branchName, CommitDto commitDto, String token) {
        String encodedContent = Base64.getEncoder().encodeToString(commitDto.content().getBytes());
        String createUrl = "https://api.github.com/repos/" + nickname + "/" + repoName + "/contents/" + commitDto.fileName();
        Map<String, Object> createRequest = Map.of(
                "message", "create " + commitDto.fileName(),
                "commiter", Map.of(
                        "name", commitDto.commiterName(),
                        "email", commitDto.commiterEmail()
                ),
                "content", encodedContent,
                "branch", branchName
        );

        HttpEntity<Map<String, Object>> createEntity = new HttpEntity<>(createRequest, createHeaders(token));

        ResponseEntity<Map> createResponse = restTemplate.exchange(
                createUrl,
                HttpMethod.PUT,
                createEntity,
                Map.class);

        return createResponse.getStatusCode().is2xxSuccessful();
    }

    private Boolean updateFile(String nickname, String repoName, String branchName, CommitDto commitDto, String encodedContent, String sha, String token) {
        String updateUrl = "https://api.github.com/repos/" + nickname + "/" + repoName + "/contents/" + commitDto.fileName();
        Map<String, Object> updateRequest = Map.of(
                "message", "update " + commitDto.fileName(),
                "content", encodedContent,
                "commiter", Map.of(
                        "name", commitDto.commiterName(),
                        "email", commitDto.commiterEmail()
                ),
                "sha", sha,
                "branch", branchName
        );

        HttpEntity<Map<String, Object>> updateEntity = new HttpEntity<>(updateRequest, createHeaders(token));

        ResponseEntity<Map> updateResponse = restTemplate.exchange(
                updateUrl,
                HttpMethod.PUT,
                updateEntity,
                Map.class);

        return updateResponse.getStatusCode().is2xxSuccessful();
    }
    public void createWebhook(Long userId, String repoName) {
        String nickname = userRepository.findById(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER))
                .getNickname();

        String token = stringEncryptor.decrypt(userRepository.findById(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER))
                .getGithubAccessToken());

        String apiUrl = String.format("https://api.github.com/repos/%s/%s/hooks", nickname, repoName);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "web");
        body.put("active", true);
        body.put("events", Arrays.asList("push", "pull_request"));

        Map<String, String> config = new HashMap<>();
        config.put("url", webhookUrl);
        config.put("content_type", "json");

        body.put("config", config);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createHeaders(token));

        restTemplate.postForEntity(apiUrl, entity, String.class);
    }
}
