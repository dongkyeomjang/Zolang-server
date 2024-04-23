package com.kcs.zolang.service;

import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.dto.response.ClusterListDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.ClusterRepository;
import com.kcs.zolang.utility.ClusterApiUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClusterService {
    private final ClusterRepository clusterRepository;
    private final ClusterApiUtil clusterApiUtil;

    public List<ClusterListDto> getClusterList(Long userId) {
        return clusterRepository.findByUserId(userId).stream() // 사용자가 웹서비스에 등록해놓은 클러스터 리스트 가져와서
                .map(ClusterListDto::fromEntity) // ClusterListDto로 변환하고
                .toList(); // List로 합친 후 리턴. 반환값: clusterName, domainUrl, version(DB에 저장되어있는 값들만) Status를 위한 값은 추가로 구현해야함(저장 값 먼저 출력 후, 약간 시간이 걸릴 수 있는 status는 로딩 후 나타나게)
    }

    public Boolean getClusterStatus(Long clusterId) {
        // 클러스터의 상태를 가져오는 메소드. 클러스터의 상태는 DB에 저장되어있지 않고, 실시간으로 가져와야함.
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_CLUSTER));
        return clusterApiUtil.getClusterStatus(cluster.getDomainUrl(), cluster.getSecretToken());
    }
}
