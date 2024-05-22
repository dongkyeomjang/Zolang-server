package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.dto.request.RegisterClusterDto;
import com.kcs.zolang.service.ClusterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cluster")
@Tag(name = "Monitoring", description = "모니터링 관련 API")
public class ClusterController {
    private final ClusterService clusterService;

    @PostMapping("")
    @Operation(summary = "클러스터 등록", description = "사용자의 클러스터를 등록")
    public ResponseDto<?> registerCluster(@UserId Long userId, @RequestBody RegisterClusterDto registerClusterDto) throws IOException {
        return ResponseDto.created(clusterService.registerCluster(userId, registerClusterDto));
    }
    @GetMapping("")
    @Operation(summary = "클러스터 목록 조회", description = "사용자의 클러스터 목록을 조회. 단, DB에 있는 값만 들고옴. 상태조회 API 별도 존재")
    public ResponseDto<?> getClusters(
            @UserId Long userId
    ) {
        return ResponseDto.ok(clusterService.getClusters(userId));
    }
    @GetMapping("/{clusterId}/status")
    @Operation(summary = "클러스터 상태 조회", description = "클러스터의 상태를 조회. 클러스터 목록 조회와 같이 호출되어야함")
    public ResponseDto<?> getClusterStatus(
            @PathVariable Long clusterId
    ) throws Exception {
        return ResponseDto.ok(clusterService.getClusterStatus(clusterId));
    }
    @GetMapping("/{cluster_id}/nodes")
    @Operation(summary = "클러스터 노드 목록 조회", description = "등록된 클러스터의 노드 목록을 조회")
    public ResponseDto<?> getClusterNodes(
            @PathVariable("cluster_id") Long clusterId
    ) throws Exception {
        return ResponseDto.ok(clusterService.getClusterNodes(clusterId));
    }

    @GetMapping("/{cluster_id}/nodes/{node_name}")
    @Operation(summary = "클러스터 노드 상세 조회", description = "등록된 클러스터의 노드 상세 정보를 조회")
    public ResponseDto<?> getClusterNode(
            @PathVariable("cluster_id") Long clusterId,
            @PathVariable("node_name") String nodeName
    ) throws Exception {
        return ResponseDto.ok(clusterService.getClusterNodeDetail(clusterId, nodeName));
    }
}
