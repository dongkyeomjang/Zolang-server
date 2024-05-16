package com.kcs.zolang.service;

import com.kcs.zolang.domain.CICD;
import com.kcs.zolang.dto.request.GitRepoRequestDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.CICDRepository;
import com.kcs.zolang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CICDService {
    private final CICDRepository cicdRepository;
    private final UserRepository userRepository;
    private final GithubService githubService;
    public void registerRepository(Long userId, GitRepoRequestDto requestDto) {
        var user = userRepository.findById(userId).orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER));

        githubService.createWebhook(userId, requestDto.repoName());

        CICD cicd = CICD.builder()
                .repositoryName(requestDto.repoName())
                .user(user)
                .build();
        cicdRepository.save(cicd);
    }
    public void handleGithubWebhook(Map<String, Object> payload) {
        String repoName = (String) ((Map<String, Object>) payload.get("repository")).get("name");
        CICD cicd = cicdRepository.findByRepositoryName(repoName)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_REPOSITORY));
        runPipeline(cicd);
    }
    private void deployApplication(CICD cicd) {
        // Kubernetes API를 사용하여 애플리케이션을 배포하는 로직
        // 예를 들어, Deployment와 Service를 생성
    }
    private void runPipeline(CICD cicd) {
        // CI/CD 파이프라인 실행 로직
        // 1. 코드를 클론하고 빌드
        // 2. Docker 이미지를 생성하고 레지스트리에 push
        // 3. Kubernetes 클러스터에 배포
        deployApplication(cicd);
    }
}
