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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateRequest;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateResponse;
import software.amazon.awssdk.services.ec2.model.RequestLaunchTemplateData;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class ClusterService {
    private final MonitoringUtil monitoringUtil;
    private static final Logger log = LoggerFactory.getLogger(NetworkService.class);
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

    @Value("${aws.security-group}")
    private String securityGroupId;

    public String getVersion(ClusterVersionRequestDto clusterVersionRequestDto) throws ApiException {
        ApiClient client = Config.fromToken("https://" + clusterVersionRequestDto.domainUrl(),
                clusterVersionRequestDto.secretToken(), false);
        Configuration.setDefaultApiClient(client);
        VersionApi versionApi = new VersionApi(client);
        return versionApi.getCode().execute().getGitVersion();
    }
    @Transactional
    public ClusterCreateResponseDto createCluster(Long userId, String clusterName) {
        // 클러스터를 EKS에 생성
        CreateClusterRequest request = CreateClusterRequest.builder()
                .name(clusterName)
                .roleArn(eksRoleArn)
                .resourcesVpcConfig(VpcConfigRequest.builder()
                        .subnetIds(subnetId1, subnetId2)
                        .securityGroupIds(securityGroupId)
                        .endpointPublicAccess(true)
                        .endpointPrivateAccess(true)
                        .build())
                .version("1.21")
                .kubernetesNetworkConfig(config -> config.ipFamily("IPV4").build())
                .build();

        CreateClusterResponse response = eksClient.createCluster(request);
        software.amazon.awssdk.services.eks.model.Cluster eksCluster = response.cluster();

        // 노드 그룹 생성
        createNodeGroup(clusterName, clusterName + "-nodegroup");

        // 클러스터 도메인 URL 가져오기
        String clusterEndpoint = eksCluster.endpoint();

        // 클러스터 정보를 DB에 저장
        Cluster cluster = clusterRepository.save(
                Cluster.builder()
                        .user(userRepository.findById(userId)
                                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER)))
                        .clusterName(clusterName)
                        .provider("zolang")
                        .secretToken("") // 초기에는 빈 값으로 설정
                        .domainUrl(clusterEndpoint)
                        .version("1.21")
                        .build()
        );

        // 클러스터 준비 상태 확인 및 서비스 계정 생성
        ClusterUtil.waitForClusterReady(cluster);

        // Kubeconfig 파일 생성 및 설정
        ClusterUtil.createKubeconfig(cluster);

        // 서비스 계정 및 역할 생성
        ClusterUtil.createServiceAccountWithKubectl(cluster);

        // 서비스 계정 토큰 가져오기
        String secretToken = ClusterUtil.getServiceAccountTokenWithKubectl(cluster);

        // 클러스터 정보 업데이트
        cluster.update(clusterName, secretToken, clusterEndpoint, "1.21");
        clusterRepository.save(cluster);

        return ClusterCreateResponseDto.fromEntity(cluster);
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
        CreateLaunchTemplateRequest request = CreateLaunchTemplateRequest.builder()
                .launchTemplateName("eks-launch-template" + UUID.randomUUID())
                .launchTemplateData(RequestLaunchTemplateData.builder()
                        .imageId("ami-0d989729759ea3477") // Amazon Linux 2 arm64 EKS Optimized AMI ID
                        .instanceType("t2.medium")
                        .keyName("bongousse")
                        .securityGroupIds(securityGroupId)
                        .userData(getUserDataScript(clusterName))
                        .build())
                .build();

        CreateLaunchTemplateResponse response = ec2Client.createLaunchTemplate(request);
        return response.launchTemplate().launchTemplateId();
    }

    private String getUserDataScript(String clusterName) { // 노드 초기화 스크립트
        return "#!/bin/bash\n" +
                "set -o xtrace\n" +
                "yum update -y\n" +
                "yum install -y aws-cli\n" +
                String.format("/etc/eks/bootstrap.sh %s", clusterName);
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
        return clusterRepository.findByUserId(userId).stream() // 사용자가 웹서비스에 등록해놓은 클러스터 리스트 가져와서
                .map(ClusterListDto::fromEntity) // ClusterListDto로 변환하고
                .toList(); // List로 합친 후 리턴. 반환값: clusterName, domainUrl, version(DB에 저장되어있는 값들만) Status를 위한 값은 추가로 구현해야함(저장 값 먼저 출력 후, 약간 시간이 걸릴 수 있는 status는 로딩 후 나타나게)
    }

    //현재 클러스터 상태
    public Boolean getClusterStatus(Long userId, Long clusterId) throws Exception {

        ApiClient client = monitoringUtil.getV1Api(userId, clusterId);
        CoreV1Api coreV1Api = new CoreV1Api(client);

        try {
            V1NodeList nodeList = coreV1Api.listNode().execute();
            List<V1Node> nodes = nodeList.getItems();

            for (V1Node node : nodes) {
                List<V1NodeCondition> conditions = node.getStatus().getConditions();
                //추후 for문 수정 예정
                for (V1NodeCondition condition : conditions) {
                    if ("Ready".equals(condition.getType())) {
                        if (!"True".equals(condition.getStatus())) {
                            return false;
                        }
                    }
                }
            }
            return true;
        } catch (ApiException e) {
            log.error("Error listing services: {}", e.getResponseBody(), e);
            //오류 코드 수정 예정
            throw new CommonException(ErrorCode.API_ERROR);
        }
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

            //실행중인 파드만 사용량으로
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



}
