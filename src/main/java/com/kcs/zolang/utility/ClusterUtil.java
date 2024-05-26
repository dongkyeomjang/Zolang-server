package com.kcs.zolang.utility;

import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.domain.CICD;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

            // 역할 생성
            String roleCommand = String.format("kubectl create role %s --namespace default --verb=get,list,watch --resource=pods,services,deployments,configmaps,secrets,networkpolicies", roleName);
            log.info("Executing command: {}", roleCommand);
            executeCommand(roleCommand);

            // 역할 바인딩 생성
            String roleBindingCommand = String.format("kubectl create rolebinding %s --namespace default --role=%s --serviceaccount=default:%s", roleBindingName, roleName, saName);
            log.info("Executing command: {}", roleBindingCommand);
            executeCommand(roleBindingCommand);
        } catch (IOException | InterruptedException e) {
            log.error("Exception occurred while creating service account and role with kubectl", e);
            throw new RuntimeException("Failed to create service account and role with kubectl", e);
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

    public void deployApplication(CICD cicd, Cluster cluster) {
        try {
            ApiClient client = buildApiClient(generateKubeConfig(cluster));
            Configuration.setDefaultApiClient(client);

            CoreV1Api api = new CoreV1Api();

            applyYamlToCluster(api, generateDeploymentYaml(cicd));
        } catch (IOException e) {
            throw new RuntimeException("Failed to deploy application", e);
        }
    }
    public void rolloutDeployment(CICD cicd, Cluster cluster) {
        try {
            ApiClient client = buildApiClient(generateKubeConfig(cluster));
            Configuration.setDefaultApiClient(client);
            AppsV1Api api = new AppsV1Api();

            V1Deployment deployment = api.readNamespacedDeployment(cicd.getRepositoryName(), "default").execute();

            Map<String, String> annotations = deployment.getSpec().getTemplate().getMetadata().getAnnotations();
            if (annotations == null) {
                annotations = new HashMap<>();
            }
            annotations.put("kubectl.kubernetes.io/restartedAt", Instant.now().toString());
            deployment.getSpec().getTemplate().getMetadata().setAnnotations(annotations);

            api.replaceNamespacedDeployment(cicd.getRepositoryName(), "default", deployment);
        } catch (IOException | ApiException e) {
            throw new RuntimeException("Failed to rollout deployment", e);
        }
    }


    public void runPipeline(CICD cicd, Cluster cluster, Boolean isFirstRun) {
        try {
            String repoUrl = String.format("https://github.com/%s/%s.git", cicd.getUser().getNickname(), cicd.getRepositoryName());
            executeCommand(String.format("git clone %s", repoUrl));
            executeCommand("./gradlew build");

            String ecrLoginCommand = String.format("aws ecr get-login-password --region %s | docker login --username AWS --password-stdin %s.dkr.ecr.%s.amazonaws.com",
                    awsRegion, awsAccountId, awsRegion);
            executeCommand(ecrLoginCommand);

            String imageName = String.format("%s.dkr.ecr.%s.amazonaws.com/%s-%s:latest",
                    awsAccountId, awsRegion, ecrRepositoryPrefix, cicd.getRepositoryName());
            executeCommand(String.format("docker build -t %s .", imageName));
            executeCommand(String.format("docker push %s", imageName));

            if (isFirstRun) {
                deployApplication(cicd, cluster);
            } else {
                rolloutDeployment(cicd, cluster);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to run pipeline", e);
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
    private void executeCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + command);
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
    private void applyYamlToCluster(CoreV1Api api, String yaml) {
        try {
            api.createNamespacedPod("default", io.kubernetes.client.util.Yaml.loadAs(yaml, io.kubernetes.client.openapi.models.V1Pod.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply YAML", e);
        }
    }
}
