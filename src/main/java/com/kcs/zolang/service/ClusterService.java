package com.kcs.zolang.service;

import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.domain.User;
import com.kcs.zolang.dto.request.RegisterClusterDto;
import com.kcs.zolang.dto.response.ClusterCreateResponseDto;
import com.kcs.zolang.dto.response.ClusterListDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.ClusterRepository;
import com.kcs.zolang.repository.UserRepository;
import com.kcs.zolang.utility.ClusterUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateRequest;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateResponse;
import software.amazon.awssdk.services.ec2.model.RequestLaunchTemplateData;
import software.amazon.awssdk.services.eks.EksClient;
import software.amazon.awssdk.services.eks.model.*;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClusterService {
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
                        .maxSize(4)
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
                                .build()
                )
                .getId();
    }
    public List<ClusterListDto> getClusters(Long userId) {
        return clusterRepository.findByUserId(userId).stream() // 사용자가 웹서비스에 등록해놓은 클러스터 리스트 가져와서
                .map(ClusterListDto::fromEntity) // ClusterListDto로 변환하고
                .toList(); // List로 합친 후 리턴. 반환값: clusterName, domainUrl, version(DB에 저장되어있는 값들만) Status를 위한 값은 추가로 구현해야함(저장 값 먼저 출력 후, 약간 시간이 걸릴 수 있는 status는 로딩 후 나타나게)
    }

    public Boolean getClusterStatus(Long clusterId) throws Exception {
        // 클러스터의 상태를 가져오는 메소드. 클러스터의 상태는 DB에 저장되어있지 않고, 실시간으로 가져와야함.
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_CLUSTER));
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + cluster.getSecretToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(cluster.getDomainUrl())
                .path("/api/v1/nodes");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                builder.build().encode().toUri(), HttpMethod.GET, entity, Map.class);

        if(response.getStatusCode() == HttpStatus.OK && response.hasBody()) {
            Map<String,Object> body = response.getBody();
            if (body != null && body.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
                for (Map<String, Object> item : items) {
                    Map<String, Object> status = (Map<String, Object>) item.get("status");
                    Map<String, Object> conditions = ((List<Map<String, Object>>) status.get("conditions")).get(0);
                    if (!"True".equals(conditions.get("status"))) {
                        return false; // 하나라도 비정상 상태면 false 반환
                    }
                }
            }
            return true; // 모든 노드가 정상 상태면 true 반환
        }
        return false;
    }

    public List<Map<String,Object>> getClusterNodes(Long clusterId) throws Exception {
        // 클러스터에 등록된 노드 목록을 가져오는 메소드. 클러스터의 노드 목록은 DB에 저장되어있지 않고, 실시간으로 가져와야함.
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_CLUSTER));
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + cluster.getSecretToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(cluster.getDomainUrl())
                .path("/api/v1/nodes");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                builder.build().encode().toUri(), HttpMethod.GET, entity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.hasBody()) {
            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> result = new ArrayList<>();  // 빈 리스트 생성 대신 ArrayList 인스턴스를 생성
            if (body != null && body.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
                for (Map<String, Object> item : items) {
                    Map<String, Object> nodeDetails = new HashMap<>();  // 각 노드의 상세 정보를 저장할 Map 생성
                    Map<String, Object> allocatableList = new HashMap<>();
                    Map<String, Object> capacityList = new HashMap<>();
                    Map<String, Object> metadata = (Map<String, Object>) item.get("metadata");
                    Map<String, Object> status = (Map<String, Object>) item.get("status");
                    Map<String, Object> nodeInfo = (Map<String, Object>) status.get("nodeInfo");
                    Map<String, Object> allocatable = (Map<String, Object>) status.get("allocatable");
                    Map<String, Object> capacity = (Map<String, Object>) status.get("capacity");
                    List<Map<String, Object>> conditions = ((List<Map<String, Object>>) status.get("conditions"));
                    allocatableList.put("allocatable-cpu", allocatable.get("cpu"));
                    allocatableList.put("allocatable-memory", allocatable.get("memory"));
                    allocatableList.put("allocatable-pods", allocatable.get("pods"));
                    nodeDetails.put("allocatable", allocatableList);
                    capacityList.put("capacity-cpu", capacity.get("cpu"));
                    capacityList.put("capacity-memory", capacity.get("memory"));
                    capacityList.put("capacity-pods", capacity.get("pods"));
                    nodeDetails.put("capacity", capacityList);
                    nodeDetails.put("created", metadata.get("creationTimestamp"));
                    nodeDetails.put("name", metadata.get("name"));
                    nodeDetails.put("conditions", conditions);
                    nodeDetails.put("KubeletVersion", nodeInfo.get("kubeletVersion"));

                    result.add(nodeDetails);  // 개별 노드의 상세 정보를 결과 리스트에 추가
                }
            }
            return result;  // 모든 노드의 상세 정보가 담긴 리스트 반환
        }
        return List.of();
    }

    public Map<String,Object> getClusterNodeDetail(Long clusterId, String nodeName) throws Exception {
        // 특정 클러스터의 노드 상세 정보를 가져오는 메소드.
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_CLUSTER));
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + cluster.getSecretToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(cluster.getDomainUrl())
                .path("/api/v1/nodes/" + nodeName);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                builder.build().encode().toUri(), HttpMethod.GET, entity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.hasBody()) {
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("metadata")) {
                Map<String, Object> nodeDetails = new HashMap<>();  // 각 노드의 상세 정보를 저장할 Map 생성
                Map<String, Object> metadata = (Map<String, Object>) body.get("metadata");
                Map<String, Object> status = (Map<String, Object>) body.get("status");
                Map<String, Object> nodeInfo = (Map<String, Object>) status.get("nodeInfo");

                nodeDetails.put("created", metadata.get("creationTimestamp"));
                nodeDetails.put("name", metadata.get("name"));
                nodeDetails.put("addresses", status.get("addresses"));
                nodeDetails.put("capacity", status.get("capacity"));
                nodeDetails.put("allocatable", status.get("allocatable"));
                nodeDetails.put("conditions", status.get("conditions"));
                nodeDetails.put("OS", nodeInfo.get("operatingSystem"));
                nodeDetails.put("OSImage", nodeInfo.get("osImage"));
                nodeDetails.put("kernelVersion", nodeInfo.get("kernelVersion"));
                nodeDetails.put("containerRuntime", nodeInfo.get("containerRuntimeVersion"));
                nodeDetails.put("KubeletVersion", nodeInfo.get("kubeletVersion"));
                return nodeDetails;
            }
        }
        return Map.of();
    }
}

