package com.kcs.zolang.service;

import com.kcs.zolang.domain.CICD;
import com.kcs.zolang.dto.request.GitRepoRequestDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.CICDRepository;
import com.kcs.zolang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CICDService {
    private final CICDRepository cicdRepository;
    private final UserRepository userRepository;
    private final GithubService githubService;

    @Value("${aws.account.id}")
    private String awsAccountId;

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.ecr.repository.prefix}")
    private String ecrRepositoryPrefix;

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
        String namespace = cicd.getUser().getNickname(); // 사용자 이름을 네임스페이스로 사용
        createNamespaceIfNotExists(namespace);

        try {
            String deploymentName = cicd.getRepositoryName(); // Deployment 이름을 레포지토리 이름으로 사용
            String imageName = String.format("%s.dkr.ecr.%s.amazonaws.com/%s-%s:latest",
                    awsAccountId, awsRegion, ecrRepositoryPrefix, cicd.getRepositoryName());

            // Deployment YAML 파일 작성
            String deploymentYaml = String.format(
                    "apiVersion: apps/v1\n" +
                            "kind: Deployment\n" +
                            "metadata:\n" +
                            "  name: %s\n" +
                            "  namespace: %s\n" +
                            "spec:\n" +
                            "  replicas: 1\n" +
                            "  selector:\n" +
                            "    matchLabels:\n" +
                            "      app: %s\n" +
                            "  template:\n" +
                            "    metadata:\n" +
                            "      labels:\n" +
                            "        app: %s\n" +
                            "    spec:\n" +
                            "      containers:\n" +
                            "      - name: %s\n" +
                            "        image: %s\n" +
                            "        ports:\n" +
                            "        - containerPort: 8080\n",
                    deploymentName, namespace, deploymentName, deploymentName, deploymentName, imageName
            );

            // YAML 파일을 kubectl을 사용하여 적용
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("sh", "-c", "echo \"" + deploymentYaml + "\" | kubectl apply -f - --namespace=" + namespace);

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Failed to deploy application");
            }

            System.out.println("Application deployed successfully");

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to deploy application", e);
        }
    }

    private void runPipeline(CICD cicd) {
        try {
            // 1. 코드를 클론하고 빌드
            String repoUrl = "https://github.com/" + cicd.getUser().getNickname() + "/" + cicd.getRepositoryName() + ".git";
            String cloneCommand = "git clone " + repoUrl;
            String buildCommand = "./gradlew build"; // Gradle 빌드 명령

            runCommand(cloneCommand);
            runCommand(buildCommand);

            // 2. ECR 로그인
            String ecrLoginCommand = String.format("aws ecr get-login-password --region %s | docker login --username AWS --password-stdin %s.dkr.ecr.%s.amazonaws.com",
                    awsRegion, awsAccountId, awsRegion);
            runCommand(ecrLoginCommand);

            // 3. Docker 이미지를 생성하고 ECR에 push
            String imageName = String.format("%s.dkr.ecr.%s.amazonaws.com/%s-%s:latest",
                    awsAccountId, awsRegion, ecrRepositoryPrefix, cicd.getRepositoryName());
            String dockerBuildCommand = "docker build -t " + imageName + " .";
            String dockerPushCommand = "docker push " + imageName;

            runCommand(dockerBuildCommand);
            runCommand(dockerPushCommand);

            // 4. Kubernetes 클러스터에 배포
            deployApplication(cicd);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to run pipeline", e);
        }
    }

    private void runCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("sh", "-c", command);

        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + command);
        }
    }

    private void createNamespaceIfNotExists(String namespace) {
        try {
            String checkNamespaceCommand = "kubectl get namespace " + namespace;
            ProcessBuilder checkProcessBuilder = new ProcessBuilder();
            checkProcessBuilder.command("sh", "-c", checkNamespaceCommand);
            Process checkProcess = checkProcessBuilder.start();
            int checkExitCode = checkProcess.waitFor();

            if (checkExitCode != 0) { // 네임스페이스가 없으면 생성
                String createNamespaceCommand = "kubectl create namespace " + namespace;
                runCommand(createNamespaceCommand);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to check or create namespace", e);
        }
    }
}
