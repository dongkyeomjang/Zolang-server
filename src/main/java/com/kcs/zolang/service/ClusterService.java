package com.kcs.zolang.service;

import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.domain.User;
import com.kcs.zolang.dto.request.ClusterVersionRequestDto;
import com.kcs.zolang.dto.request.RegisterClusterDto;
import com.kcs.zolang.dto.response.ClusterCreateResponseDto;
import com.kcs.zolang.dto.response.cluster.*;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.ClusterRepository;
import com.kcs.zolang.repository.UserRepository;
import com.kcs.zolang.utility.ClusterUtil;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.custom.NodeMetrics;
import io.kubernetes.client.custom.NodeMetricsList;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.VersionApi;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.eks.EksClient;
import software.amazon.awssdk.services.eks.model.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.kcs.zolang.utility.MonitoringUtil;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.kubernetes.client.openapi.models.V1NodeList;
import software.amazon.awssdk.services.eks.model.LaunchTemplateSpecification;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClusterService {
    private final MonitoringUtil monitoringUtil;
    private final ClusterUtil clusterUtil;
    private final ClusterRepository clusterRepository;
    private final UserRepository userRepository;
    private final EksClient eksClient;
    private final Ec2Client ec2Client;

    @Value("${aws.eks-role.arn}")
    private String eksRoleArn;

    @Value("${aws.ec2-role.arn}")
    private String ec2RoleArn;

    @Value("${aws.subnet.id-1}")
    private String subnetId1;

    @Value("${aws.subnet.id-2}")
    private String subnetId2;

    @Value("${aws.security-group.id-1}")
    private String securityGroupId1;

    @Value("${aws.vpc.id}")
    private String vpcId;

    @Value("${aws.version}")
    private String version;

    public String getVersion(ClusterVersionRequestDto clusterVersionRequestDto) throws ApiException {
        ApiClient client = Config.fromToken("https://" + clusterVersionRequestDto.domainUrl(),
                clusterVersionRequestDto.secretToken(), false);
        Configuration.setDefaultApiClient(client);
        VersionApi versionApi = new VersionApi(client);
        return versionApi.getCode().execute().getGitVersion();
    }

    public ClusterCreateResponseDto createCluster(Long userId, String clusterName) {
        String userEmail;
        // 클러스터 상태를 'creating'으로 설정하고 DB에 저장
        clusterRepository.findByProviderAndUserId("zolang", userId)
                .ifPresent(cluster -> {
                    throw new CommonException(ErrorCode.ALREADY_EXIST_ZOLANG_CLUSTER);
                });
        if(!(userRepository.findById(userId).get().getEmail() ==null)){
            userEmail = userRepository.findById(userId).get().getEmail();
        } else {
            userEmail = "none";
        }

        Cluster cluster = clusterRepository.save(
                Cluster.builder()
                        .user(userRepository.findById(userId)
                                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER)))
                        .clusterName(clusterName)
                        .provider("zolang")
                        .secretToken("")
                        .domainUrl("")
                        .version("")
                        .status("creating")
                        .build()
        );

        // 비동기적으로 클러스터 생성
        createClusterAsync(cluster.getId(), clusterName, userEmail);

        return ClusterCreateResponseDto.fromEntity(cluster);
    }

    @Async
    public void createClusterAsync(Long clusterId, String clusterName, String userEmail) {
        try {
            tagSecurityGroup(clusterName, securityGroupId1);
            // 클러스터를 EKS에 생성
            CreateClusterRequest request = CreateClusterRequest.builder()
                    .name(clusterName)
                    .roleArn(eksRoleArn)
                    .resourcesVpcConfig(VpcConfigRequest.builder()
                            .subnetIds(subnetId1, subnetId2)
                            .securityGroupIds(securityGroupId1)
                            .endpointPublicAccess(true)
                            .endpointPrivateAccess(true)
                            .build())
                    .version(version)
                    .kubernetesNetworkConfig(config -> config.ipFamily("IPV4").build())
                    .build();

            eksClient.createCluster(request);

            // 클러스터 생성 상태 확인 및 노드 그룹 생성
            waitForClusterToBeActiveAndCreateNodeGroup(clusterName, clusterName + "-nodegroup");

            // 생성이 완료되면 클러스터 정보를 다시 가져옴
            DescribeClusterRequest describeClusterRequest = DescribeClusterRequest.builder()
                    .name(clusterName)
                    .build();

            DescribeClusterResponse response = eksClient.describeCluster(describeClusterRequest);

            software.amazon.awssdk.services.eks.model.Cluster eksCluster = response.cluster();

            // Kubeconfig 파일 생성 및 설정
            Cluster cluster = clusterRepository.findById(clusterId)
                    .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_CLUSTER));
            clusterUtil.createKubeconfig(cluster,userEmail);

            // 서비스 계정 및 역할 생성
            clusterUtil.createServiceAccountWithKubectl(cluster);

            // 서비스 계정 토큰 가져오기
            String secretToken = clusterUtil.getServiceAccountTokenWithKubectl(cluster);

            // 도메인 url에서 https:// 제거
            String domainUrl = eksCluster.endpoint().replace("https://", "");

            // 클러스터 정보 업데이트
            cluster.update(clusterName, secretToken, domainUrl, version);
            cluster.updateStatus("ready");
            clusterRepository.save(cluster);

            clusterUtil.installAndConfigureMetricsServer();

        } catch (Exception e) {
            log.error("Exception occurred while creating cluster asynchronously", e);
            Cluster cluster = clusterRepository.findById(clusterId)
                    .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_CLUSTER));
            clusterRepository.delete(cluster);
        }
    }

    private void createNodeGroup(String clusterName, String nodegroupName) {
        // 노드 그룹을 생성할 때 사용할 Launch Template 생성
        String launchTemplateId = createLaunchTemplate(clusterName);

        CreateNodegroupRequest nodegroupRequest = CreateNodegroupRequest.builder()
                .clusterName(clusterName)
                .nodegroupName(nodegroupName)
                .nodeRole(ec2RoleArn)
                .subnets(subnetId1, subnetId2)
                .launchTemplate(LaunchTemplateSpecification.builder()
                        .id(launchTemplateId)
                        .version("$Latest")
                        .build())
                .scalingConfig(NodegroupScalingConfig.builder()
                        .minSize(2)
                        .maxSize(2)
                        .desiredSize(2)
                        .build())
                .build();
        eksClient.createNodegroup(nodegroupRequest);
    }

    private String createLaunchTemplate(String clusterName) {
        String userDataScript = getUserDataScript(clusterName);
        String base64UserData = Base64.getEncoder().encodeToString(userDataScript.getBytes());

        CreateLaunchTemplateRequest request = CreateLaunchTemplateRequest.builder()
                .launchTemplateName("eks-launch-template" + UUID.randomUUID())
                .launchTemplateData(RequestLaunchTemplateData.builder()
                        .imageId("ami-0d989729759ea3477")
                        .instanceType("t4g.medium")
                        .keyName("bongousse")
                        .securityGroupIds(securityGroupId1)
                        .userData(base64UserData)
                        .build())
                .build();

        CreateLaunchTemplateResponse response = ec2Client.createLaunchTemplate(request);
        return response.launchTemplate().launchTemplateId();
    }

    private void tagSecurityGroup(String clusterName, String securityGroupId) {
        Tag clusterTag = Tag.builder()
                .key("kubernetes.io/cluster/" + clusterName)
                .value("owned")
                .build();

        CreateTagsRequest createTagsRequest = CreateTagsRequest.builder()
                .resources(securityGroupId)
                .tags(clusterTag)
                .build();

        ec2Client.createTags(createTagsRequest);
    }

    private String getUserDataScript(String clusterName) {
        return "#!/bin/bash\n" +
                "set -o xtrace\n" +
                "yum update -y\n" +
                "yum install -y aws-cli\n" +
                String.format("/etc/eks/bootstrap.sh %s", clusterName);
    }

    @Async
    public void waitForClusterToBeActiveAndCreateNodeGroup(String clusterName, String nodegroupName) {
        try {
            while (true) {
                DescribeClusterRequest describeClusterRequest = DescribeClusterRequest.builder()
                        .name(clusterName)
                        .build();

                DescribeClusterResponse describeClusterResponse = eksClient.describeCluster(describeClusterRequest);
                software.amazon.awssdk.services.eks.model.Cluster cluster = describeClusterResponse.cluster();

                if ("ACTIVE".equals(cluster.statusAsString())) {
                    break;
                }

                Thread.sleep(30000);
            }
            createNodeGroup(clusterName, nodegroupName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    @Transactional
    public Long registerCluster(Long userId, RegisterClusterDto registerClusterDto) throws IOException {
        // 이미 존재하는 클러스터를 등록.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER));
        return clusterRepository.save(
                Cluster.builder()
                        .clusterName(registerClusterDto.clusterName())
                        .domainUrl(registerClusterDto.domainUrl())
                        .secretToken(registerClusterDto.secretToken())
                        .user(user)
                        .version(registerClusterDto.version())
                        .provider("external")
                        .build()
        )
                .getId();
    }


    public List<ClusterListDto> getClusters(Long userId) {
        return clusterRepository.findByUserId(userId).stream()
                .peek(cluster -> {
                    if (!"creating".equals(cluster.getStatus())) {
                        CoreV1Api coreV1Api = new CoreV1Api(monitoringUtil.getV1Api(userId, cluster.getId()));
                        boolean hasError = false;
                        try {
                            List<V1Node> nodes = coreV1Api.listNode().execute().getItems();
                            for (V1Node node : nodes) {
                                V1NodeStatus status = node.getStatus();
                                if (status == null) {
                                    hasError = true;
                                    break;
                                }
                                List<V1NodeCondition> conditions = status.getConditions();
                                if (conditions == null) {
                                    hasError = true;
                                    break;
                                }
                                for (V1NodeCondition condition : conditions) {
                                    if ("Ready".equals(condition.getType()) && !"True".equals(condition.getStatus())) {
                                        hasError = true;
                                        break;
                                    }
                                }
                                if (hasError) break;
                            }
                        } catch (ApiException e) {
                            hasError = true;
                        }
                        cluster.updateStatus(hasError ? "error" : "ready");
                        clusterRepository.save(cluster);
                    } // creating(생성중) 상태 클러스터는 상태 업데함트 안함
                })
                .map(ClusterListDto::fromEntityAddHttps)
                .toList();
    }
    //노드 사용량
    private NodeUsageDto getNodeUsage(NodeMetrics nodeMetrics) {
        return NodeUsageDto.fromEntity(nodeMetrics, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    //노드 리스트
    public List<ClusterNodeListDto> getClusterNodeList(Long userId, Long clusterId) throws Exception {
        ApiClient client = monitoringUtil.getV1Api(userId, clusterId);
        CoreV1Api coreV1Api = new CoreV1Api(client);
        Metrics metricsApi = new Metrics(client);

        try {
            V1NodeList nodeList = coreV1Api.listNode().execute();
            List<V1Node> nodes = nodeList.getItems();
            NodeMetricsList metricsList = metricsApi.getNodeMetrics();

            return nodes.stream()
                    .map(node -> {
                        NodeMetrics nodeMetrics = metricsList.getItems().stream()
                                .filter(metric -> metric.getMetadata().getName().equals(node.getMetadata().getName()))
                                .findFirst()
                                .orElse(null);
                        NodeUsageDto usage = nodeMetrics != null ? getNodeUsage(nodeMetrics) : null;
                        return ClusterNodeListDto.fromEntity(node, usage);
                    })
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }


    //노드 디테일
    public List<ClusterNodeDetailDto> getClusterNodeDetail(Long userId, Long clusterId, String nodeName) throws Exception {
        ApiClient client = monitoringUtil.getV1Api(userId, clusterId);
        CoreV1Api coreV1Api = new CoreV1Api(client);
        try {
            V1NodeList nodeList = coreV1Api.listNode().execute();
            List<V1Node> nodes = nodeList.getItems();
            List<ClusterNodeDetailDto> nodeDetailDtos = new ArrayList<>();
            for (V1Node node : nodes) {
                if (node.getMetadata().getName().equals(nodeName)) {
                    nodeDetailDtos.add(ClusterNodeDetailDto.fromEntity(node));
                }
            }
            if (nodeDetailDtos.isEmpty()) {
                throw new CommonException(ErrorCode.NOT_FOUND_NETWORK);
            }
            return nodeDetailDtos;
        } catch (ApiException e) {
            //오류 코드 수정 예정
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    //클러스터 사용량(노드 합)
    public ClusterStatusDto getClusterUsage(Long userId, Long clusterId) throws Exception {
        ApiClient client = monitoringUtil.getV1Api(userId, clusterId);
        CoreV1Api coreV1Api = new CoreV1Api(client);
        Metrics metricsApi = new Metrics(client);

        try {
            V1NodeList nodeList = coreV1Api.listNode().execute();
            List<V1Node> nodes = nodeList.getItems();
            List<V1Pod> podList = coreV1Api.listPodForAllNamespaces().execute().getItems();
            NodeMetricsList metricsList = metricsApi.getNodeMetrics();

            double totalCpuUsage = 0;
            double totalCpuAllocatable = 0;
            double totalCpuCapacity = 0;
            long totalMemoryUsage = 0;
            long totalMemoryAllocatable = 0;
            long totalMemoryCapacity = 0;
            int totalPodUsage = 0;
            int totalPodAllocatable = 0;
            int totalPodCapacity = 0;

            for (V1Node node : nodes) {
                NodeMetrics nodeMetrics = metricsList.getItems().stream()
                        .filter(metric -> metric.getMetadata().getName().equals(node.getMetadata().getName()))
                        .findFirst()
                        .orElse(null);

                if (nodeMetrics != null) {
                    NodeUsageDto usage = getNodeUsage(nodeMetrics);
                    totalCpuUsage += usage.nodeCpuUsage();
                    totalMemoryUsage += usage.nodeMemoryUsage();
                }

                totalCpuAllocatable += Double.parseDouble(node.getStatus().getAllocatable().get("cpu").getNumber().toString());
                totalMemoryAllocatable += node.getStatus().getAllocatable().get("memory").getNumber().longValue();
                totalPodAllocatable += Integer.parseInt(node.getStatus().getAllocatable().get("pods").getNumber().toString());

                totalCpuCapacity += Double.parseDouble(node.getStatus().getCapacity().get("cpu").getNumber().toString());
                totalMemoryCapacity += node.getStatus().getCapacity().get("memory").getNumber().longValue();
                totalPodCapacity += Integer.parseInt(node.getStatus().getCapacity().get("pods").getNumber().toString());
            }

            //실행중인 파드만 사용량으로(running이 아닐 경우 그냥 0으로)
            totalPodUsage = (int) podList.stream()
                    .filter(pod -> "Running".equals(pod.getStatus().getPhase()))
                    .count();

            return ClusterStatusDto.builder()
                    .cpuUsage(totalCpuUsage)
                    .cpuAllocatable(totalCpuAllocatable)
                    .cpuCapacity(totalCpuCapacity)
                    .memoryUsage(totalMemoryUsage)
                    .memoryAllocatable(totalMemoryAllocatable)
                    .memoryCapacity(totalMemoryCapacity)
                    .podUsage(totalPodUsage)
                    .podAllocatable(totalPodAllocatable)
                    .podCapacity(totalPodCapacity)
                    .build();
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }


    //각 노드 사용량
    public List<ClusterNodeSimpleDto> getClusterNodeSimpleStatus(Long userId, Long clusterId, String nodeName) throws Exception {
        ApiClient client = monitoringUtil.getV1Api(userId, clusterId);
        CoreV1Api coreV1Api = new CoreV1Api(client);
        Metrics metricsApi = new Metrics(client);

        try {
            V1NodeList nodeList = coreV1Api.listNode().execute();
            List<V1Node> nodes = nodeList.getItems();
            NodeMetricsList metricsList = metricsApi.getNodeMetrics();
            List<ClusterNodeSimpleDto> clusterNodeSimpleDtos = new ArrayList<>();

            for (V1Node node : nodes) {
                if (node.getMetadata().getName().equals(nodeName)) {
                    NodeMetrics nodeMetrics = metricsList.getItems().stream()
                            .filter(metric -> metric.getMetadata().getName().equals(node.getMetadata().getName()))
                            .findFirst()
                            .orElse(null);
                    NodeUsageDto usage = nodeMetrics != null ? getNodeUsage(nodeMetrics) : null;
                    ClusterNodeListDto nodeListDto = ClusterNodeListDto.fromEntity(node, usage);
                    clusterNodeSimpleDtos.add(ClusterNodeSimpleDto.fromEntity(nodeListDto));
                }
            }

            if (clusterNodeSimpleDtos.isEmpty()) {
                throw new CommonException(ErrorCode.NOT_FOUND_NETWORK);
            }
            return clusterNodeSimpleDtos;
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }
    public void deleteCluster(Long userId, Long clusterId) {
        Cluster cluster = clusterRepository.findByIdAndUserId(clusterId, userId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_CLUSTER));
        clusterRepository.delete(cluster);
    }
}
