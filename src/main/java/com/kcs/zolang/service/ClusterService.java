package com.kcs.zolang.service;

import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.domain.User;
import com.kcs.zolang.dto.request.RegisterClusterDto;
import com.kcs.zolang.dto.response.ClusterCreateResponseDto;
import com.kcs.zolang.dto.response.cluster.ClusterListDto;
import com.kcs.zolang.dto.response.cluster.ClusterNodeDetailDto;
import com.kcs.zolang.dto.response.cluster.ClusterNodeListDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.ClusterRepository;
import com.kcs.zolang.repository.UserRepository;
import com.kcs.zolang.utility.ClusterUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateRequest;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateResponse;
import software.amazon.awssdk.services.ec2.model.RequestLaunchTemplateData;
import software.amazon.awssdk.services.eks.EksClient;
import software.amazon.awssdk.services.eks.model.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.kcs.zolang.utility.MonitoringUtil;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.kubernetes.client.openapi.models.V1NodeList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class ClusterService {
    private final MonitoringUtil monitoringUtil;
    private final ClusterUtil clusterUtil;
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

    @Value("${aws.security-group.id-1}")
    private String securityGroupId1;

    @Value("${aws.security-group.id-2}")
    private String securityGroupId2;

    @Value("${aws.version}")
    private String version;

    @Transactional
    public ClusterCreateResponseDto createCluster(Long userId, String clusterName) {
        // 클러스터를 EKS에 생성
        CreateClusterRequest request = CreateClusterRequest.builder()
                .name(clusterName)
                .roleArn(eksRoleArn)
                .resourcesVpcConfig(VpcConfigRequest.builder()
                        .subnetIds(subnetId1, subnetId2)
                        .securityGroupIds(securityGroupId1, securityGroupId2)
                        .endpointPublicAccess(true)
                        .endpointPrivateAccess(true)
                        .build())
                .version(version)
                .kubernetesNetworkConfig(config -> config.ipFamily("IPV4").build())
                .build();

        eksClient.createCluster(request);

        // 클러스터 정보를 DB에 저장
        Cluster cluster = clusterRepository.save(
                Cluster.builder()
                        .user(userRepository.findById(userId)
                                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER)))
                        .clusterName(clusterName)
                        .provider("zolang")
                        .secretToken("") // 초기에는 빈 값으로 설정
                        .domainUrl("") // 초기에는 빈 값으로 설정
                        .version("") // 초기에는 빈 값으로 설정
                        .build()
        );

        waitForClusterToBeActiveAndCreateNodeGroup(clusterName, clusterName + "-nodegroup");

        // 생성이 완료되면 클러스터 정보를 다시 가져옴
        DescribeClusterRequest describeClusterRequest = DescribeClusterRequest.builder()
                .name(clusterName)
                .build();

        DescribeClusterResponse response = eksClient.describeCluster(describeClusterRequest);

        software.amazon.awssdk.services.eks.model.Cluster eksCluster = response.cluster();


        // Kubeconfig 파일 생성 및 설정
        clusterUtil.createKubeconfig(cluster);

        // 서비스 계정 및 역할 생성
        clusterUtil.createServiceAccountWithKubectl(cluster);

        // 서비스 계정 토큰 가져오기
        String secretToken = clusterUtil.getServiceAccountTokenWithKubectl(cluster);

        // 도메인 url에서 https:// 제거
        String domainUrl = eksCluster.endpoint().replace("https://", "");

        // 클러스터 정보 업데이트
        cluster.update(clusterName, secretToken, domainUrl, version);
        clusterRepository.save(cluster);

        clusterUtil.installAndConfigureMetricsServer();

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
        String userDataScript = getUserDataScript(clusterName);
        String base64UserData = Base64.getEncoder().encodeToString(userDataScript.getBytes());

        CreateLaunchTemplateRequest request = CreateLaunchTemplateRequest.builder()
                .launchTemplateName("eks-launch-template" + UUID.randomUUID())
                .launchTemplateData(RequestLaunchTemplateData.builder()
                        .imageId("ami-0d989729759ea3477") // Amazon Linux 2 arm64 EKS Optimized AMI ID
                        .instanceType("t4g.medium")
                        .keyName("bongousse")
                        .securityGroupIds(securityGroupId1, securityGroupId2)
                        .userData(base64UserData)
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

                Thread.sleep(30000); // 30초 대기
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
        return clusterRepository.findByUserId(userId).stream() // 사용자가 웹서비스에 등록해놓은 클러스터 리스트 가져와서
                .map(ClusterListDto::fromEntity) // ClusterListDto로 변환하고
                .toList(); // List로 합친 후 리턴. 반환값: clusterName, domainUrl, version(DB에 저장되어있는 값들만) Status를 위한 값은 추가로 구현해야함(저장 값 먼저 출력 후, 약간 시간이 걸릴 수 있는 status는 로딩 후 나타나게)
    }

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

    public List<ClusterNodeListDto> getClusterNodeList(Long userId, Long clusterId) throws Exception {

        ApiClient client = monitoringUtil.getV1Api(userId, clusterId);
        CoreV1Api coreV1Api = new CoreV1Api(client);

        try {
            V1NodeList nodeList = coreV1Api.listNode().execute();
            List<V1Node> nodes = nodeList.getItems();

            List<ClusterNodeListDto> nodeListDtos = nodes.stream()
                    .map(ClusterNodeListDto::fromEntity)
                    .collect(Collectors.toList());
            return nodeListDtos;
        } catch (ApiException e) {
            //오류 코드 수정 예정
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

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
}
