package com.kcs.zolang.service;

import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.domain.User;
import com.kcs.zolang.dto.request.RegisterClusterDto;
import com.kcs.zolang.dto.response.cluster.ClusterListDto;
import com.kcs.zolang.dto.response.cluster.ClusterNodeDetailDto;
import com.kcs.zolang.dto.response.cluster.ClusterNodeListDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.ClusterRepository;
import com.kcs.zolang.repository.UserRepository;
import com.kcs.zolang.utility.MonitoringUtil;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.kubernetes.client.openapi.models.V1NodeList;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClusterService {
    private final ClusterRepository clusterRepository;
    private final UserRepository userRepository;
    private final MonitoringUtil monitoringUtil;
    private static final Logger log = LoggerFactory.getLogger(NetworkService.class);

    public Long registerCluster(Long userId,RegisterClusterDto registerClusterDto) throws IOException {
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
