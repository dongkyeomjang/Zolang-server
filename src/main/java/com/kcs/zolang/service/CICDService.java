package com.kcs.zolang.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kcs.zolang.domain.*;
import com.kcs.zolang.dto.request.CICDRequestDto;
import com.kcs.zolang.dto.request.EnvVarDto;
import com.kcs.zolang.dto.response.BuildDto;
import com.kcs.zolang.dto.response.CICDDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.*;
import com.kcs.zolang.utility.ClusterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
@RequiredArgsConstructor
public class CICDService {
    private final CICDRepository cicdRepository;
    private final BuildRepository buildRepository;
    private final UserRepository userRepository;
    private final ClusterRepository clusterRepository;
    private final EnvVarRepository envVarRepository;
    private final RestTemplate restTemplate;
    private final ClusterUtil clusterUtil;
    private final StringEncryptor stringEncryptor;

    @Value("${github.webhook-url}")
    private String webhookUrl;

    public void registerRepository(Long userId, CICDRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER));
        Cluster clusterProvidedByZolang = clusterRepository.findByProviderAndUserId("zolang", userId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_CLUSTER)); // 우선 클러스터 생성을 한 이후에만 배포 등록이 가능함

        String nickname = user.getNickname();
        String token = stringEncryptor.decrypt(user.getGithubAccessToken());

        String apiUrl = String.format("https://api.github.com/repos/%s/%s/hooks", nickname, requestDto.repoName());
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "web");
            body.put("active", true);
            body.put("events", requestDto.trigger());

            Map<String, String> config = new HashMap<>();
            config.put("url", webhookUrl);
            config.put("content_type", "json");

            body.put("config", config);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + token);
            headers.set("X-GitHub-Api-Version", "2022-11-28");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            log.info("Sending request to GitHub API to create webhook: {}", apiUrl);
            log.info("Request headers: {}", headers);
            log.info("Request body: {}", body);

            restTemplate.postForEntity(apiUrl, entity, String.class);

            CICD cicd = CICD.builder()
                    .user(user)
                    .repositoryName(requestDto.repoName())
                    .branch(requestDto.branch())
                    .language(requestDto.language())
                    .languageVersion(requestDto.version())
                    .buildTool(requestDto.buildTool())
                    .trigger(String.join(",", requestDto.trigger()))
                    .build();
            cicdRepository.save(cicd);
            for (EnvVarDto envVarDto : requestDto.envVars()){
                EnvironmentVariable environmentVariable = EnvironmentVariable.builder()
                        .key(envVarDto.key())
                        .value(envVarDto.value())
                        .CICD(cicd)
                        .build();
                envVarRepository.save(environmentVariable);
            }
            Build build = Build.builder()
                            .CICD(cicd)
                            .buildStatus("building")
                            .lastCommitMessage("init")
                            .buildNumber(1)
                            .build();
            buildRepository.save(build);

            try {
                List<EnvironmentVariable> environmentVariables = envVarRepository.findByCICDId(cicd.getId());
                clusterUtil.runPipeline(cicd, environmentVariables, clusterProvidedByZolang, true).get();  // 비동기 작업 완료 대기
                build.update("success");
                buildRepository.save(build);
            } catch (Exception e) {
                build.update("failed");
                buildRepository.save(build);
                throw new CommonException(ErrorCode.PIPELINE_ERROR);
            }
        } catch (HttpClientErrorException e) {
            throw new CommonException(ErrorCode.FAILED_CREATE_WEBHOOK);
        }
    }

    public void handleGithubWebhook(Map<String, Object> payload, String eventType) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.convertValue(payload, JsonNode.class);

            if (!"push".equals(eventType) && !"pull_request".equals(eventType)) {
                log.info("Ignoring event: {}", eventType);
                return;
            }

            JsonNode repositoryNode = rootNode.path("repository");
            if (repositoryNode.isMissingNode() || !repositoryNode.has("name")) {
                throw new CommonException(ErrorCode.INVALID_PAYLOAD);
            }
            String repoName = repositoryNode.path("name").asText();

            String branch;
            String lastCommitMessage;

            if ("push".equals(eventType)) {
                branch = rootNode.path("ref").asText().replace("refs/heads/", "");
                lastCommitMessage = rootNode.path("head_commit").path("message").asText();
            } else {
                JsonNode pullRequestNode = rootNode.path("pull_request");
                branch = pullRequestNode.path("head").path("ref").asText();
                lastCommitMessage = pullRequestNode.path("head").path("sha").asText();
            }

            log.info("Branch name: {}", branch);
            log.info("Last commit message: {}", lastCommitMessage);

            CICD cicd = cicdRepository.findByRepositoryName(repoName)
                    .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_REPOSITORY));
            log.info("Found CICD: {}", cicd);
            if (!cicd.getBranch().equals(branch)) {
                log.info("Ignoring webhook event for branch: {}", branch);
                return;
            }

            Long userId = cicd.getUser().getId();
            log.info("User ID: {}", userId);
            Cluster clusterProvidedByZolang = clusterRepository.findByProviderAndUserId("zolang", userId)
                    .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_CLUSTER));
            log.info("Found cluster: {}", clusterProvidedByZolang);
            Build build = Build.builder()
                    .CICD(cicd)
                    .lastCommitMessage(lastCommitMessage)
                    .buildNumber(buildRepository.findBuildNumberByCICD(cicd) + 1)
                    .buildStatus("building")
                    .build();
            buildRepository.save(build);
            try {
                List<EnvironmentVariable> environmentVariables = envVarRepository.findByCICDId(cicd.getId());
                log.info("Environment variables: {}", environmentVariables);
                clusterUtil.runPipeline(cicd, environmentVariables, clusterProvidedByZolang, false).get();  // 비동기 작업 완료 대기
                build.update("success");
                buildRepository.save(build);
            } catch (Exception e) {
                build.update("failed");
                buildRepository.save(build);
                log.error("Failed to process pipeline: {}", e.getMessage());
                throw new CommonException(ErrorCode.PIPELINE_ERROR);
            }
        } catch (Exception e) {
            log.error("Failed to process webhook event: {}", e.getMessage());
            throw new CommonException(ErrorCode.FAILED_PROCESS_WEBHOOK);
        }
    }

    public List<CICDDto> getCICDs(Long userId) {
        List<CICD> cicdList = cicdRepository.findByUserId(userId);
        return cicdList.stream().map(cicd -> {
            Build lastBuild = buildRepository.findTopByCICDOrderByCreatedAtDesc(cicd)
                    .orElse(Build.builder().build());
            return CICDDto.fromEntity(cicd, lastBuild);
        }).toList();
    }
    public List<BuildDto> getBuildRecords(Long cicdId) {
        CICD cicd = cicdRepository.findById(cicdId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_REPOSITORY));
        List<Build> buildList = buildRepository.findByCICD(cicd);
        return buildList.stream().map(BuildDto::fromEntity).toList();
    }

    public void deleteRepository(Long userId, Long cicdId) {
        CICD cicd = cicdRepository.findById(cicdId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_REPOSITORY));
        if (!cicd.getUser().getId().equals(userId)) {
            throw new CommonException(ErrorCode.NOT_FOUND_REPOSITORY);
        }
        cicdRepository.delete(cicd);
    }
}
