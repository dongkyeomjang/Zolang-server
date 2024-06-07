package com.kcs.zolang.service;

import com.kcs.zolang.dto.request.CommitDto;
import com.kcs.zolang.dto.response.GitBranchDto;
import com.kcs.zolang.dto.response.GitRepoDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class GithubService {
    private final UserRepository userRepository;
    private final StringEncryptor stringEncryptor;
    private final RestTemplate restTemplate;

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + token);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public List<GitRepoDto> getRepositories(Long userId) {
        String token = getUserGithubToken(userId);
        List<GitRepoDto> allRepos = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();
        String nextUrl = "https://api.github.com/user/repos";

        while (nextUrl != null) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(nextUrl)
                    .queryParam("per_page", 100);  // 최대 100개의 리포지토리를 페이지 당 요청

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    builder.build().encode().toUri(),
                    HttpMethod.GET,
                    new HttpEntity<>(null, createHeaders(token)),
                    (Class<List<Map<String, Object>>>)(Class<?>)List.class);

            List<Map<String, Object>> repos = response.getBody();
            if (repos != null) {
                allRepos.addAll(repos.stream()
                        .filter(repo -> {
                            Map<String, Object> owner = (Map<String, Object>) repo.get("owner");
                            return "User".equals(owner.get("type"));
                        })
                        .map(repo -> GitRepoDto.builder()
                                .name(repo.get("name").toString())
                                .branchesUrl(repo.get("branches_url").toString())
                                .build())
                        .toList());
            }
            nextUrl = getNextUrl(response.getHeaders());
        }

        return allRepos;
    }

    private String getNextUrl(HttpHeaders headers) {
        List<String> links = headers.get(HttpHeaders.LINK);
        if (links == null || links.isEmpty()) {
            return null;
        }

        return Arrays.stream(links.get(0).split(","))
                .map(String::trim)
                .filter(link -> link.endsWith("rel=\"next\""))
                .findFirst()
                .map(link -> link.substring(link.indexOf('<') + 1, link.indexOf('>')))
                .orElse(null);
    }

    public List<GitBranchDto> getBranches(Long userId, String repoName) {
        try {
            String token = getUserGithubToken(userId);
            String nickname = getUserNickname(userId);

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://api.github.com/repos/" + nickname + "/" + repoName + "/branches");

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    builder.build().encode().toUri(),
                    HttpMethod.GET,
                    new HttpEntity<>(null, createHeaders(token)),
                    (Class<List<Map<String, Object>>>) (Class<?>) List.class);

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
        } catch (HttpClientErrorException.NotFound e) {
            throw new CommonException(ErrorCode.NOT_FOUND_REPOSITORY);
        }
    }

    public Boolean createCommit(Long userId, String repoName, String branchName, CommitDto commitDto) {
        String nickname = getUserNickname(userId);
        String token = getUserGithubToken(userId);

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
                "committer", Map.of(
                        "name", commitDto.committerName(),
                        "email", commitDto.committerEmail()
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
                "committer", Map.of(
                        "name", commitDto.committerName(),
                        "email", commitDto.committerEmail()
                ),
                "sha", sha,
                "branch", branchName
        );

        HttpEntity<Map<String, Object>> updateEntity = new HttpEntity<>(updateRequest, createHeaders(token));

        try {
            restTemplate.put(updateUrl, updateEntity);
            return true;
        } catch (HttpClientErrorException e) {
            // 오류 응답 코드 처리
            return false;
        }
    }

    private String getUserNickname(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER))
                .getNickname();
    }

    private String getUserGithubToken(Long userId) {
        return stringEncryptor.decrypt(userRepository.findById(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER))
                .getGithubAccessToken());
    }
}
