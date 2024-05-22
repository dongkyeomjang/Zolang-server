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
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ClusterUtil {

    @Value("${aws.account.id}")
    private static String awsAccountId;

    @Value("${aws.region}")
    private static String awsRegion;

    @Value("${aws.ecr.repository.prefix}")
    private static String ecrRepositoryPrefix;

    private static ApiClient buildApiClient(String kubeConfigContent) throws IOException {
        KubeConfig config = KubeConfig.loadKubeConfig(new StringReader(kubeConfigContent));
        return ClientBuilder.kubeconfig(config).build();
    }

    public static void waitForClusterReady(Cluster cluster) {
        try {
            // 클러스터가 준비될 때까지 대기
            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c",
                    "aws eks wait cluster-active --name " + cluster.getClusterName());
            Process process = processBuilder.start();
            process.waitFor(10, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("Failed to wait for cluster to be ready", e);
        }
    }

    public static void createKubeconfig(Cluster cluster) {
        String command = String.format("aws eks update-kubeconfig --name %s --region %s", cluster.getClusterName(), awsRegion);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
            processBuilder.environment().put("AWS_ACCESS_KEY_ID", System.getenv("AWS_ACCESS_KEY_ID"));
            processBuilder.environment().put("AWS_SECRET_ACCESS_KEY", System.getenv("AWS_SECRET_ACCESS_KEY"));

            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Failed to update kubeconfig");
            }

            System.out.println("Kubeconfig updated successfully");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to update kubeconfig", e);
        }
    }


    public static void createServiceAccountWithKubectl(Cluster cluster) {
        try {
            String clusterName = cluster.getClusterName();
            // 서비스 계정 생성
            String saCommand = String.format(
                    "kubectl create sa %s-sa --namespace default", clusterName);
            executeCommand(saCommand);

            // 역할 생성
            String roleCommand = String.format(
                    "kubectl create role %s-role --namespace default --verb=get,list,watch --resource=pods,services,deployments,configmaps,secrets,networkpolicies,workflows", clusterName);
            executeCommand(roleCommand);

            // 역할 바인딩 생성
            String roleBindingCommand = String.format(
                    "kubectl create rolebinding %s-rolebinding --namespace default --role=%s-role --serviceaccount=default:%s-sa", clusterName, clusterName, clusterName);
            executeCommand(roleBindingCommand);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to create service account and role with kubectl", e);
        }
    }

    public static String getServiceAccountTokenWithKubectl(Cluster cluster) {
        try {
            String clusterName = cluster.getClusterName();
            // 서비스 계정 토큰을 가져오기 위한 시크릿 이름 가져오기
            String getSecretNameCommand = String.format(
                    "kubectl get sa %s-sa --namespace default -o jsonpath='{.secrets[0].name}'", clusterName);
            String secretName = executeCommandAndGetOutput(getSecretNameCommand);

            // 시크릿 토큰 가져오기
            String getTokenCommand = String.format(
                    "kubectl get secret %s --namespace default -o jsonpath='{.data.token}' | base64 --decode", secretName);
            return executeCommandAndGetOutput(getTokenCommand);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to get service account token with kubectl", e);
        }
    }

    public static void deployApplication(CICD cicd, Cluster cluster) {
        try {
            ApiClient client = buildApiClient(generateKubeConfig(cluster));
            Configuration.setDefaultApiClient(client);

            CoreV1Api api = new CoreV1Api();

            applyYamlToCluster(api, generateDeploymentYaml(cicd));
        } catch (IOException e) {
            throw new RuntimeException("Failed to deploy application", e);
        }
    }
    public static void rolloutDeployment(CICD cicd, Cluster cluster) {
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


    public static void runPipeline(CICD cicd, Cluster cluster, Boolean isFirstRun) {
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

    private static String generateDeploymentYaml(CICD cicd) {
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
    private static String generateKubeConfig(Cluster cluster) {
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
    private static void executeCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + command);
        }
    }

    private static String executeCommandAndGetOutput(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
        Process process = processBuilder.start();
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.readLine().trim();
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + command);
        }
        return output;
    }
    private static void applyYamlToCluster(CoreV1Api api, String yaml) {
        try {
            api.createNamespacedPod("default", io.kubernetes.client.util.Yaml.loadAs(yaml, io.kubernetes.client.openapi.models.V1Pod.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply YAML", e);
        }
    }
}
