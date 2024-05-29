package com.kcs.zolang.utility;

import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.domain.CICD;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class ClusterUtil {
    @Value("${aws.account.id}")
    private String awsAccountId;
    @Value("${aws.region}")
    private String awsRegion;
    @Value("${aws.ecr.repository.prefix}")
    private String ecrRepositoryPrefix;

    private ApiClient buildApiClient(String kubeConfigContent) throws IOException {
        KubeConfig config = KubeConfig.loadKubeConfig(new StringReader(kubeConfigContent));
        return ClientBuilder.kubeconfig(config).build();
    }

    public void createKubeconfig(Cluster cluster) {
        String command = String.format("aws eks update-kubeconfig --name %s --region %s", cluster.getClusterName(), awsRegion);

        try {
            log.info("createKubeconfig 진입");
            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
            log.info("processBuilder 생성");
            log.info("환경변수 설정 완료 AWS_ACCESS_KEY_ID : {}, AWS_SECRET_ACCESS : {}",System.getenv("AWS_ACCESS_KEY_ID"), System.getenv("AWS_SECRET_ACCESS_KEY"));
            processBuilder.environment().put("AWS_ACCESS_KEY_ID", System.getenv("AWS_ACCESS_KEY_ID"));
            processBuilder.environment().put("AWS_SECRET_ACCESS_KEY", System.getenv("AWS_SECRET_ACCESS_KEY"));
            log.info("환경변수 설정 완료 AWS_ACCESS_KEY_ID : {}, AWS_SECRET_ACCESS : {}", System.getenv("AWS_ACCESS_KEY_ID"), System.getenv("AWS_SECRET_ACCESS_KEY"));

            Process process = processBuilder.start();
            log.info("process 실행");

            // 표준 출력 읽기
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            log.info("BufferedReader 생성 (stdout)");
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("stdout: {}", line);
            }

            // 에러 출력 읽기
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            log.info("BufferedReader 생성 (stderr)");
            while ((line = errorReader.readLine()) != null) {
                log.error("stderr: {}", line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Failed to update kubeconfig");
            }

            log.info("Kubeconfig updated successfully");
        } catch (IOException | InterruptedException e) {
            log.error("Exception occurred while updating kubeconfig", e);
            throw new RuntimeException("Failed to update kubeconfig", e);
        }
    }


    public void createServiceAccountWithKubectl(Cluster cluster) {
        try {
            String clusterName = cluster.getClusterName();
            String saName = clusterName.toLowerCase().replaceAll("[^a-z0-9]", "") + "sa";
            String roleName = clusterName.toLowerCase().replaceAll("[^a-z0-9]", "") + "role";
            String roleBindingName = clusterName.toLowerCase().replaceAll("[^a-z0-9]", "") + "rolebinding";

            log.info("Creating service account with name: {}", saName);
            // 서비스 계정 생성
            String saCommand = String.format("kubectl create sa %s --namespace default", saName);
            log.info("Executing command: {}", saCommand);
            executeCommand(saCommand);

            // 시크릿 생성
            String secretCommand = String.format("kubectl apply -f - <<EOF\n" +
                    "apiVersion: v1\n" +
                    "kind: Secret\n" +
                    "metadata:\n" +
                    "  name: %s-token\n" +
                    "  namespace: default\n" +
                    "  annotations:\n" +
                    "    kubernetes.io/service-account.name: \"%s\"\n" +
                    "type: kubernetes.io/service-account-token\n" +
                    "EOF", saName, saName);
            log.info("Executing command: {}", secretCommand);
            executeCommand(secretCommand);

            // 역할 생성 (YAML 사용)
            String roleCommand = String.format("kubectl apply -f - <<EOF\n" +
                    "apiVersion: rbac.authorization.k8s.io/v1\n" +
                    "kind: ClusterRole\n" +
                    "metadata:\n" +
                    "  name: %s\n" +
                    "rules:\n" +
                    "- apiGroups: [\"\"]\n" +
                    "  resources: [\"pods\", \"services\", \"deployments\", \"configmaps\", \"secrets\", \"networkpolicies\", \"nodes\"]\n" +
                    "  verbs: [\"get\", \"list\", \"watch\"]\n" +
                    "- apiGroups: [\"metrics.k8s.io\"]\n" +
                    "  resources: [\"pods\", \"services\", \"deployments\", \"configmaps\", \"secrets\", \"networkpolicies\", \"nodes\"]\n" +
                    "  verbs: [\"get\", \"list\", \"watch\"]\n" +
                    "EOF", roleName);
            log.info("Executing command: {}", roleCommand);
            executeCommand(roleCommand);

            // 역할 바인딩 생성
            String roleBindingCommand = String.format("kubectl create clusterrolebinding %s --clusterrole=%s --serviceaccount=default:%s", roleBindingName, roleName, saName);
            log.info("Executing command: {}", roleBindingCommand);
            executeCommand(roleBindingCommand);
        } catch (IOException | InterruptedException e) {
            log.error("Exception occurred while creating service account and role with kubectl", e);
            throw new RuntimeException("Failed to create service account and role with kubectl", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public String getServiceAccountTokenWithKubectl(Cluster cluster) {
        try {
            String clusterName = cluster.getClusterName();
            String saName = clusterName.toLowerCase().replaceAll("[^a-z0-9]", "") + "sa";
            String secretName = saName + "-token";

            // 서비스 계정 토큰을 가져오기 위한 시크릿 이름 가져오기
            String getSecretNameCommand = String.format("kubectl get secrets --namespace default -o jsonpath=\"{.items[?(@.metadata.annotations['kubernetes\\.io/service-account\\.name']=='%s')].metadata.name}\"", saName);
            log.info("Executing command: {}", getSecretNameCommand);
            String actualSecretName = executeCommandAndGetOutput(getSecretNameCommand);

            if (actualSecretName.isEmpty()) {
                throw new RuntimeException("Failed to get secret name for service account: " + saName);
            }

            // 시크릿 토큰 가져오기
            String getTokenCommand = String.format("kubectl get secret %s --namespace default -o jsonpath='{.data.token}' | base64 -d", actualSecretName);
            log.info("Executing command: {}", getTokenCommand);
            return executeCommandAndGetOutput(getTokenCommand);
        } catch (IOException | InterruptedException e) {
            log.error("Exception occurred while getting service account token with kubectl", e);
            throw new RuntimeException("Failed to get service account token with kubectl", e);
        }
    }
    public void rolloutDeployment(CICD cicd, Cluster cluster) {
        try {
            ApiClient client = buildApiClient(generateKubeConfig(cluster));
            Configuration.setDefaultApiClient(client);
            AppsV1Api api = new AppsV1Api();

            // Deployment 삭제
            api.deleteNamespacedDeployment(cicd.getRepositoryName(), "default");
            log.info("Deleted existing deployment: {}", cicd.getRepositoryName());

            // 새로운 Deployment 생성
            String deploymentYaml = generateDeploymentYaml(cicd);
            applyYamlToCluster(deploymentYaml, cluster);
            log.info("Applied new deployment: {}", cicd.getRepositoryName());

        } catch (IOException e) {
            throw new CommonException(ErrorCode.PIPELINE_ERROR);
        }
    }

    @Async
    public CompletableFuture<Void> runPipeline(CICD cicd, Cluster cluster, Boolean isFirstRun) {
        try {
            String repoUrl = String.format("https://github.com/%s/%s.git", cicd.getUser().getNickname(), cicd.getRepositoryName());
            String repoDir = "/app/resources/repo/" + cicd.getRepositoryName();

            if (!isFirstRun) {
                executeCommand(String.format("cd %s && git pull", repoDir));
            } else {
                executeCommand(String.format("cd /app/resources/repo && git clone %s %s", repoUrl, repoDir));
            }

            if (!new File(repoDir + "/gradlew").exists() || !new File(repoDir + "/gradle/wrapper/gradle-wrapper.jar").exists()) {
                log.info("Gradle wrapper 없음. 생성 중");
                executeCommand("cd " + repoDir + " && gradle wrapper --gradle-version 8.0.2");
            }

            executeCommand("cd " + repoDir + " && ./gradlew build -x test");

            String ecrLoginCommand = String.format("aws ecr get-login-password --region %s | docker login --username AWS --password-stdin %s.dkr.ecr.%s.amazonaws.com",
                    awsRegion, awsAccountId, awsRegion);
            executeCommand(ecrLoginCommand);

            String ecrRepoName = cicd.getRepositoryName().toLowerCase().replaceAll("[^a-z0-9]", "");

            String imageName = String.format("%s.dkr.ecr.%s.amazonaws.com/%s-%s:latest",
                    awsAccountId, awsRegion, ecrRepositoryPrefix, ecrRepoName);
            createEcrRepositoryIfNotExists(String.format("%s-%s", ecrRepositoryPrefix, ecrRepoName));
            executeCommand(String.format("cd %s && docker build -t %s .", repoDir, imageName));
            executeCommand(String.format("docker push %s", imageName));

            if (isFirstRun) {
                applyYamlToCluster(generateDeploymentYaml(cicd), cluster);
            } else {
                rolloutDeployment(cicd, cluster);
            }
            return CompletableFuture.completedFuture(null);
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new CommonException(ErrorCode.PIPELINE_ERROR);
        }
    }

    private String generateDeploymentYaml(CICD cicd) {
        String deploymentName = cicd.getRepositoryName();
        String imageName = String.format("%s.dkr.ecr.%s.amazonaws.com/%s-%s:latest",
                awsAccountId, awsRegion, ecrRepositoryPrefix, cicd.getRepositoryName());
        return String.format(
                "apiVersion: apps/v1\n" +
                        "kind: Deployment\n" +
                        "metadata:\n" +
                        "  name: %s\n" +
                        "  namespace: default\n" +
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
                deploymentName, deploymentName, deploymentName, deploymentName, imageName);
    }
    private String generateKubeConfig(Cluster cluster) {
        return String.format(
                "apiVersion: v1\n" +
                        "clusters:\n" +
                        "- cluster:\n" +
                        "    server: %s\n" +
                        "  name: %s\n" +
                        "contexts:\n" +
                        "- context:\n" +
                        "    cluster: %s\n" +
                        "    user: %s\n" +
                        "  name: %s\n" +
                        "current-context: %s\n" +
                        "kind: Config\n" +
                        "preferences: {}\n" +
                        "users:\n" +
                        "- name: %s\n" +
                        "  user:\n" +
                        "    token: %s\n",
                cluster.getDomainUrl(),
                cluster.getClusterName(),
                cluster.getClusterName(),
                cluster.getClusterName(),
                cluster.getClusterName(),
                cluster.getClusterName(),
                cluster.getClusterName(),
                cluster.getSecretToken()
        );
    }
    private void executeCommand(String command) throws IOException, InterruptedException, ExecutionException {
        log.info("Executing command: {}", command);
        ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
        processBuilder.redirectErrorStream(true); // stderr와 stdout을 같이 출력
        Process process = processBuilder.start();

        // 별도의 쓰레드로 출력 읽기
        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), log);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<?> future = executorService.submit(outputGobbler);

        boolean finished = process.waitFor(60, TimeUnit.MINUTES); // 빌드 시간을 충분히 길게 설정
        if (!finished) {
            process.destroy();
            executorService.shutdownNow();
            throw new RuntimeException("Command timed out: " + command);
        }

        future.get(); // 출력 쓰레드 완료 대기
        executorService.shutdown();

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code " + exitCode + ": " + command);
        }
    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private org.slf4j.Logger log;

        public StreamGobbler(InputStream inputStream, org.slf4j.Logger log) {
            this.inputStream = inputStream;
            this.log = log;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("stdout: {}", line);
                }
            } catch (IOException e) {
                log.error("Error reading process output", e);
            }
        }
    }

    private String executeCommandAndGetOutput(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + command);
        }
        return output.toString().trim();
    }
    private void applyYamlToCluster(String yaml, Cluster cluster) {
        try {
            // YAML 파일을 읽어서 여러 개의 Kubernetes 리소스 객체로 변환
            List<Object> resources = io.kubernetes.client.util.Yaml.loadAll(yaml);
            ApiClient client = buildApiClient(generateKubeConfig(cluster));
            Configuration.setDefaultApiClient(client);

            // 각 리소스 객체를 적절한 API를 사용하여 Kubernetes 클러스터에 적용
            for (Object resource : resources) {
                if (resource instanceof io.kubernetes.client.openapi.models.V1Deployment) {
                    AppsV1Api appsV1Api = new AppsV1Api();
                    V1Deployment deployment = (V1Deployment) resource;
                    appsV1Api.createNamespacedDeployment("default", deployment);
                } else if (resource instanceof io.kubernetes.client.openapi.models.V1Service) {
                    CoreV1Api coreV1Api = new CoreV1Api();
                    V1Service service = (V1Service) resource;
                    coreV1Api.createNamespacedService("default", service);
                } else if (resource instanceof io.kubernetes.client.openapi.models.V1Pod) {
                    CoreV1Api coreV1Api = new CoreV1Api();
                    V1Pod pod = (V1Pod) resource;
                    coreV1Api.createNamespacedPod("default", pod);
                } else if (resource instanceof io.kubernetes.client.openapi.models.V1ConfigMap) {
                    CoreV1Api coreV1Api = new CoreV1Api();
                    V1ConfigMap configMap = (V1ConfigMap) resource;
                    coreV1Api.createNamespacedConfigMap("default", configMap);
                }
                // 필요한 경우 다른 리소스 타입에 대한 처리 추가
                else {
                    throw new RuntimeException("Unsupported resource type: " + resource.getClass().getName());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply YAML", e);
        }
    }

    private void createEcrRepositoryIfNotExists(String repoName) throws IOException, InterruptedException, ExecutionException {
        String checkRepoExistsCommand = String.format("aws ecr describe-repositories --repository-names %s --region %s", repoName, awsRegion);
        log.info("Executing command: {}", checkRepoExistsCommand);
        String createRepoCommand = String.format("aws ecr create-repository --repository-name %s --region %s", repoName, awsRegion);
        log.info("Executing command: {}", createRepoCommand);

        Process process = Runtime.getRuntime().exec(checkRepoExistsCommand);
        process.waitFor();
        if (process.exitValue() != 0) {
            executeCommand(createRepoCommand);
        }
    }

    public void installAndConfigureMetricsServer() {
        try {
            String applyCommand = "kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml";
            log.info("Executing command: {}", applyCommand);
            executeCommand(applyCommand);

            String patchCommand = "kubectl patch deployment metrics-server -n kube-system --type='json' -p='[{" +
                    "\"op\": \"add\", \"path\": \"/spec/template/spec/containers/0/args/-\", \"value\": \"--kubelet-insecure-tls\"}," +
                    "{\"op\": \"add\", \"path\": \"/spec/template/spec/hostNetwork\", \"value\": true}]'";
            log.info("Executing command: {}", patchCommand);
            executeCommand(patchCommand);

            log.info("Metrics Server installed and configured successfully");
        } catch (IOException | InterruptedException | ExecutionException e) {
            log.error("Exception occurred while installing and configuring metrics-server", e);
            throw new RuntimeException("Failed to install and configure metrics-server", e);
        }
    }
}
