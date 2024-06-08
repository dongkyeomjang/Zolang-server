package com.kcs.zolang.utility;

import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.domain.EnvironmentVariable;
import com.kcs.zolang.dto.response.CICDDto;
import com.kcs.zolang.dto.response.UserCICDDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.utility.BuildTool.*;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClusterUtil {
    @Value("${aws.account.id}")
    private String awsAccountId;
    @Value("${aws.region}")
    private String awsRegion;
    @Value("${aws.ecr.repository.prefix}")
    private String ecrRepositoryPrefix;
    @Value("${email}")
    private String email;
    private final JobQueueUtility jobQueueUtility;
    private final StringEncryptor stringEncryptor;

    public void createKubeconfig(Cluster cluster, String userEmail, Boolean isCreatingCluster) {
        String command = String.format("aws eks update-kubeconfig --name %s --region %s", cluster.getClusterName(), awsRegion);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
            processBuilder.environment().put("AWS_ACCESS_KEY_ID", System.getenv("AWS_ACCESS_KEY_ID"));
            processBuilder.environment().put("AWS_SECRET_ACCESS_KEY", System.getenv("AWS_SECRET_ACCESS_KEY"));

            Process process = processBuilder.start();

            // 표준 출력 읽기
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("stdout: {}", line);
            }

            // 에러 출력 읽기
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                log.error("stderr: {}", line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Failed to update kubeconfig");
            }
            log.info("Kubeconfig updated successfully");
            if(isCreatingCluster) {
                // 인그레스 컨트롤러 설치
                installIngressController();
                // ClusterIssuer 생성
                installClusterIssuer(userEmail);
            }
        } catch (IOException | InterruptedException e) {
            throw new CommonException(ErrorCode.FAILED_CREATE_KUBECONFIG);
        }
    }

    public void createServiceAccountWithKubectl(Cluster cluster) {
        try {
            String clusterName = cluster.getClusterName().toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
            String saName = clusterName + "sa";
            String roleName = clusterName + "role";
            String roleBindingName = clusterName + "rolebinding";

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
                    "  resources: [\"pods\", \"services\", \"deployments\", \"configmaps\", \"secrets\", \"networkpolicies\", \"nodes\", \"namespaces\"]\n" +
                    "  verbs: [\"get\", \"list\", \"watch\",\"create\",\"update\",\"delete\"]\n" +
                    "- apiGroups: [\"apps\"]\n" +
                    "  resources: [\"deployments\", \"replicasets\", \"daemonsets\", \"statefulsets\"]\n" +
                    "  verbs: [\"get\", \"list\", \"watch\",\"create\",\"update\",\"delete\"]\n" +
                    "- apiGroups: [\"batch\"]\n" +
                    "  resources: [\"jobs\", \"cronjobs\"]\n" +
                    "  verbs: [\"get\", \"list\", \"watch\",\"create\",\"update\",\"delete\"]\n" +
                    "- apiGroups: [\"metrics.k8s.io\"]\n" +
                    "  resources: [\"pods\", \"services\", \"deployments\", \"configmaps\", \"secrets\", \"networkpolicies\", \"nodes\",\"namespaces\"]\n" +
                    "  verbs: [\"get\", \"list\", \"watch\"]\n" +
                    "- apiGroups: [\"networking.k8s.io\"]\n" +
                    "  resources: [\"ingresses\", \"ingressclasses\"]\n" +
                    "  verbs: [\"get\", \"list\", \"watch\",\"create\",\"update\",\"delete\"]\n" +
                    "EOF", roleName);
            log.info("Executing command: {}", roleCommand);

            executeCommand(roleCommand);

            // 역할 바인딩 생성
            String roleBindingCommand = String.format("kubectl create clusterrolebinding %s --clusterrole=%s --serviceaccount=default:%s", roleBindingName, roleName, saName);
            log.info("Executing command: {}", roleBindingCommand);
            executeCommand(roleBindingCommand);
        } catch (IOException | InterruptedException | ExecutionException e){
            throw new CommonException(ErrorCode.FAILED_CREATE_SA_ROLE_ROLEBINDING);
        }
    }

    public String getServiceAccountTokenWithKubectl(Cluster cluster) {
        try {
            String clusterName = cluster.getClusterName();
            String saName = clusterName.toLowerCase().replaceAll("[^a-z0-9]", "") + "sa";

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
            throw new CommonException(ErrorCode.FAILED_GET_SA_TOKEN);
        }
    }
    @Async("taskExecutor")
    public CompletableFuture<Void> runPipeline(CICDDto cicdDto, List<EnvironmentVariable> environmentVariables, Cluster cluster, UserCICDDto userCICDDto, Boolean isFirstRun) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        jobQueueUtility.addJob(userCICDDto.id(), () -> {
            try {
                String repoName = cicdDto.repositoryName().toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
                String repoUrl = String.format("https://%s@github.com/%s/%s.git",stringEncryptor.decrypt(userCICDDto.githubAccessToken()), userCICDDto.nickname(), cicdDto.repositoryName());
                String repoDir = "/app/resources/repo/" + cicdDto.repositoryName();

                executeCommand(String.format("cd /app/resources/repo && git clone %s %s", repoUrl, repoDir));

                BuildTool buildTool;

                if(cicdDto.buildTool()==null){ // python일 경우
                    buildTool = BuildToolFactory.detectBuildTool(repoDir, "none");
                } else{
                    buildTool = BuildToolFactory.detectBuildTool(repoDir, cicdDto.buildTool());
                }

                String setupCommand = buildTool.setup(repoDir);

                if (setupCommand != null) {
                    executeCommand(setupCommand);
                }

                String buildCommand = buildTool.build(repoDir);

                // python인경우 buildCommand가 null
                if(buildCommand != null) {
                    executeCommand(buildCommand);
                }

                String ecrLoginCommand = String.format("aws ecr get-login-password --region %s | docker login --username AWS --password-stdin %s.dkr.ecr.%s.amazonaws.com",
                        awsRegion, awsAccountId, awsRegion);
                executeCommand(ecrLoginCommand);

                String ecrRepoName = cicdDto.repositoryName().toLowerCase().replaceAll("[^a-z0-9]", "");

                String imageName = String.format("%s.dkr.ecr.%s.amazonaws.com/%s-%s:latest",
                        awsAccountId, awsRegion, ecrRepositoryPrefix, ecrRepoName);
                createEcrRepositoryIfNotExists(String.format("%s-%s", ecrRepositoryPrefix, ecrRepoName));
                executeCommand("docker buildx create --use && docker buildx inspect --bootstrap");
                executeCommand(String.format("cd %s && docker buildx build --platform linux/arm64 -t %s --push .", repoDir, imageName));

                if (isFirstRun) {
                    String firstYaml = generateYaml(repoName, environmentVariables, cicdDto.port(), cicdDto.serviceDomain());
                    applyYamlToCluster(firstYaml, cluster);
                    createCertificate(cicdDto.serviceDomain(), repoName, userCICDDto.email(), cluster);
                } else {
                    rolloutDeployment(repoName, cluster, environmentVariables, cicdDto.port());
                }
                executeCommand(String.format("rm -rf %s", repoDir));
                future.complete(null);
            } catch (IOException | InterruptedException | ExecutionException e) {
                log.error("Exception occurred while running pipeline", e);
                future.completeExceptionally(new CommonException(ErrorCode.PIPELINE_ERROR));
            }
        });

        return future;
    }

    private void createCertificate(String serviceDomain, String repoName, String userEmail, Cluster cluster) {
        if(userEmail.equals("none")){
            userEmail = email;
        }
        if(!serviceDomain.equals("none")) {
            try {
                createKubeconfig(cluster, userEmail, false);
                String command = String.format(
                        "kubectl apply -f - <<EOF\n" +
                                "apiVersion: cert-manager.io/v1\n" +
                                "kind: Certificate\n" +
                                "metadata:\n" +
                                "  name: %s-tls\n" +
                                "  namespace: default\n" +
                                "spec:\n" +
                                "  secretName: %s-tls\n" +
                                "  issuerRef:\n" +
                                "    name: letsencrypt-prod\n" +
                                "    kind: ClusterIssuer\n" +

                                "  dnsNames:\n" +
                                "  - %s", repoName, repoName, serviceDomain);
                executeCommand(command);
            } catch (IOException | InterruptedException | ExecutionException e) {
                throw new CommonException(ErrorCode.FAILED_CREATE_CERTIFICATE);
            }
        }
    }

    private void installIngressController() {
        try {
            String command = "helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx && " +
                    "helm repo update && " +
                    "helm install ingress-nginx ingress-nginx/ingress-nginx --namespace ingress-nginx --create-namespace";
            executeCommand(command);
            log.info("NGINX Ingress Controller installed successfully");
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new CommonException(ErrorCode.FAILED_INSTALL_INGRESS_CONTROLLER);
        }
    }
    private void installClusterIssuer(String userEmail) {
        try {
            String installCertManagerCommand = """
                    kubectl apply --validate=false -f https://github.com/jetstack/cert-manager/releases/download/v1.8.0/cert-manager.crds.yaml
                    kubectl create namespace cert-manager
                    helm repo add jetstack https://charts.jetstack.io
                    helm repo update
                    helm install cert-manager jetstack/cert-manager --namespace cert-manager --version v1.8.0""";
            executeCommand(installCertManagerCommand);
            if(userEmail.equals("none")) {
                userEmail = email;
            }
            String command = String.format(
                    "kubectl apply -f - <<EOF\n" +
                            "apiVersion: cert-manager.io/v1\n" +
                            "kind: ClusterIssuer\n" +
                            "metadata:\n" +
                            "  name: letsencrypt-prod\n" +
                            "spec:\n" +
                            "  acme:\n" +
                            "    server: https://acme-v02.api.letsencrypt.org/directory\n" +
                            "    email: %s\n" +
                            "    privateKeySecretRef:\n" +
                            "      name: letsencrypt-prod\n" +
                            "    solvers:\n" +
                            "    - http01:\n" +
                            "        ingress:\n" +
                            "          class: nginx\n" +
                            "EOF", userEmail);
            executeCommand(command);
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new CommonException(ErrorCode.FAILED_CREATE_CLUSTER_ISSUER);
        }
    }


    private String generateYaml(String repoName, List<EnvironmentVariable> environmentVariables, Integer port, String serviceDomain) {
        StringBuilder envVarsBuilder = new StringBuilder();
        if (environmentVariables != null && !environmentVariables.isEmpty()) {
            envVarsBuilder.append("        env:\n");
            for (EnvironmentVariable environmentVariable : environmentVariables) {
                envVarsBuilder.append(String.format(
                        "        - name: %s\n" +
                                "          value: %s\n", environmentVariable.getKey(), environmentVariable.getValue()));
            }
        }

        String imageName = String.format("%s.dkr.ecr.%s.amazonaws.com/%s-%s:latest",
                awsAccountId, awsRegion, ecrRepositoryPrefix, repoName);
        String deploymentYaml = String.format(
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
                        "        - containerPort: %d\n" +
                        "%s",
                repoName, repoName, repoName, repoName, imageName, port, envVarsBuilder.toString());

        String serviceYaml = String.format(
                "apiVersion: v1\n" +
                        "kind: Service\n" +
                        "metadata:\n" +
                        "  name: %s-service\n" +
                        "  namespace: default\n" +
                        "spec:\n" +
                        "  type: LoadBalancer\n" +
                        "  ports:\n" +
                        "  - port: 80\n" +
                        "    targetPort: %d\n" +
                        "  selector:\n" +
                        "    app: %s\n",
                repoName, port, repoName);

        String ingressYaml = "";
        if (!serviceDomain.equals("none")) {
            ingressYaml = String.format(
                    "apiVersion: networking.k8s.io/v1\n" +
                            "kind: Ingress\n" +
                            "metadata:\n" +
                            "  name: %s-ingress\n" +
                            "  namespace: default\n" +
                            "  annotations:\n" +
                            "    nginx.ingress.kubernetes.io/rewrite-target: /\n" +
                            "spec:\n" +
                            "  ingressClassName: nginx\n" +
                            "  rules:\n" +
                            "  - host: %s\n" +
                            "    http:\n" +
                            "      paths:\n" +
                            "      - path: /\n" +
                            "        pathType: Prefix\n" +
                            "        backend:\n" +
                            "          service:\n" +
                            "            name: %s-service\n" +
                            "            port:\n" +
                            "              number: 80\n" +
                            "  tls:\n" +
                            "  - hosts:\n" +
                            "    - %s\n" +
                            "    secretName: %s-tls\n",
                    repoName, serviceDomain, repoName, serviceDomain, repoName);
        }
        return deploymentYaml + "\n---\n" + serviceYaml + (ingressYaml.isEmpty() ? "" : "\n---\n" + ingressYaml);
    }
    private String generateDeploymentYaml(String repoName, List<EnvironmentVariable> environmentVariables, Integer port) {
        StringBuilder envVarsBuilder = new StringBuilder();
        if (environmentVariables != null && !environmentVariables.isEmpty()) {
            envVarsBuilder.append("        env:\n");
            for (EnvironmentVariable environmentVariable : environmentVariables) {
                envVarsBuilder.append(String.format(
                        "        - name: %s\n" +
                                "          value: %s\n", environmentVariable.getKey(), environmentVariable.getValue()));
            }
        }

        String imageName = String.format("%s.dkr.ecr.%s.amazonaws.com/%s-%s:latest",
                awsAccountId, awsRegion, ecrRepositoryPrefix, repoName);
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
                        "        - containerPort: %d\n" +
                        "%s",
                repoName, repoName, repoName, repoName, imageName, port, envVarsBuilder.toString());
    }
    private void applyYamlToCluster(String yaml, Cluster cluster) {
        log.info("Applying YAML to cluster: {}", yaml);
        try {
            // YAML 파일을 읽어서 여러 개의 Kubernetes 리소스 객체로 변환
            List<Object> resources = io.kubernetes.client.util.Yaml.loadAll(yaml);
            ApiClient client = Config.fromToken("https://" + cluster.getDomainUrl(), cluster.getSecretToken(),false);
            Configuration.setDefaultApiClient(client);

            // 각 리소스 객체를 적절한 API를 사용하여 Kubernetes 클러스터에 적용
            for (Object resource : resources) {
                if (resource instanceof V1Deployment deployment) {
                    AppsV1Api appsV1Api = new AppsV1Api();
                    AppsV1Api.APIcreateNamespacedDeploymentRequest request = appsV1Api.createNamespacedDeployment("default", deployment);
                    request.execute();
                } else if (resource instanceof V1Service service) {
                    CoreV1Api coreV1Api = new CoreV1Api();
                    CoreV1Api.APIcreateNamespacedServiceRequest request = coreV1Api.createNamespacedService("default", service);
                    request.execute();
                } else if (resource instanceof V1Ingress ingress) {
                    NetworkingV1Api networkingV1Api = new NetworkingV1Api();
                    NetworkingV1Api.APIcreateNamespacedIngressRequest request = networkingV1Api.createNamespacedIngress("default", ingress);
                    request.execute();
                }
                else if (resource instanceof V1Pod pod) {
                    CoreV1Api coreV1Api = new CoreV1Api();
                    CoreV1Api.APIcreateNamespacedPodRequest request = coreV1Api.createNamespacedPod("default", pod);
                    request.execute();
                } else if (resource instanceof V1ConfigMap configMap) {
                    CoreV1Api coreV1Api = new CoreV1Api();
                    CoreV1Api.APIcreateNamespacedConfigMapRequest request = coreV1Api.createNamespacedConfigMap("default", configMap);
                    request.execute();
                }
                else {
                    throw new CommonException(ErrorCode.INVALID_YAML);
                }
            }
        } catch (Exception e) {
            throw new CommonException(ErrorCode.FAILED_APPLY_YAML);
        }
    }

    public void rolloutDeployment(String repoName, Cluster cluster, List<EnvironmentVariable> environmentVariables, Integer port) {
        ApiClient client = Config.fromToken("https://" +cluster.getDomainUrl(), cluster.getSecretToken(),false);
        Configuration.setDefaultApiClient(client);
        AppsV1Api appsV1Api = new AppsV1Api();

        // Deployment 삭제
        AppsV1Api.APIdeleteNamespacedDeploymentRequest request = appsV1Api.deleteNamespacedDeployment(repoName, "default");
        try{
            request.execute();
            log.info("Deleted existing deployment: {}", repoName);
        } catch
        (Exception e) {
            log.error("Failed to delete deployment: {}", repoName);
        }

        // 새로운 yaml그룹 생성
        String newYaml = generateDeploymentYaml(repoName, environmentVariables, port);
        applyYamlToCluster(newYaml, cluster);
        log.info("Applied new deployment: {}", repoName);
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
            throw new CommonException(ErrorCode.FAILED_INSTALL_METRICS_SERVER);
        }
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
}
