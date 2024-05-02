package com.kcs.zolang.service;

import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.domain.User;
import com.kcs.zolang.dto.request.RegisterClusterDto;
import com.kcs.zolang.dto.response.ClusterListDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.ClusterRepository;
import com.kcs.zolang.repository.UserRepository;
import com.kcs.zolang.utility.ClusterApiUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class ClusterService {
    private final ClusterRepository clusterRepository;
    private final ClusterApiUtil clusterApiUtil;
    private final UserRepository userRepository;

    public Long registerCluster(Long userId, MultipartFile file, RegisterClusterDto registerClusterDto) throws IOException {
        InputStream certificateStream = file.getInputStream();
        String filename = file.getOriginalFilename();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER));
        Path certPath = Path.of("/Users/kyeom/Development/resources/cert-zolang/", userId.toString()+registerClusterDto.clusterName()+filename);
        Files.copy(certificateStream, certPath, StandardCopyOption.REPLACE_EXISTING);
        return clusterRepository.save(
                Cluster.builder()
                        .clusterName(registerClusterDto.clusterName())
                        .domainUrl(registerClusterDto.domainUrl())
                        .secretToken(registerClusterDto.secretToken())
                        .user(user)
                        .version(registerClusterDto.version())
                        .certPath(certPath.toString())
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
        RestTemplate restTemplate = clusterApiUtil.createRestTemplate(cluster);
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
        RestTemplate restTemplate = clusterApiUtil.createRestTemplate(cluster);
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
                    Map<String, Object> metadata = (Map<String, Object>) item.get("metadata");
                    Map<String, Object> status = (Map<String, Object>) item.get("status");
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

                    result.add(nodeDetails);  // 개별 노드의 상세 정보를 결과 리스트에 추가
                }
            }
            return result;  // 모든 노드의 상세 정보가 담긴 리스트 반환
        }
        return List.of();
    }
}
