package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.dto.request.ClusterVersionRequestDto;
import com.kcs.zolang.dto.request.RegisterClusterDto;
import com.kcs.zolang.service.ClusterService;
import io.kubernetes.client.openapi.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cluster")
@Tag(name = "Monitoring-cluster API", description = "모니터링-클러스터 API")
public class ClusterController {
    private final ClusterService clusterService;

    @PostMapping("/version")
    @Operation(summary = "클러스터 버전 조회", description = "클러스터 버전을 조회합니다.")
    public ResponseDto<?> getVersion(@RequestBody ClusterVersionRequestDto clusterVersionRequestDto) throws ApiException {
        return ResponseDto.ok(clusterService.getVersion(clusterVersionRequestDto));
    }

    @PostMapping("")
    @Operation(summary = "클러스터 등록", description = "사용자의 클러스터를 등록")
    public ResponseDto<?> registerCluster(
            @UserId Long userId,
            @RequestBody RegisterClusterDto registerClusterDto
            ) throws IOException {
        return ResponseDto.created(clusterService.registerCluster(userId, registerClusterDto));
    }

    @PostMapping("/{cluster_name}")
    @Operation(summary = "클러스터 생성(Zolang으로부터 제공받은 클러스터)", description = "Zolang으로부터 제공받은 클러스터를 생성")
    public ResponseDto<?> createCluster(
            @UserId Long userId,
            @PathVariable("cluster_name") String clusterName
    ){
        return ResponseDto.created(clusterService.createCluster(userId, clusterName));
    }

    @GetMapping("")
    @Operation(summary = "클러스터 목록 조회", description = "사용자의 클러스터 목록을 조회. 단, DB에 있는 값만 들고옴. 상태조회 API 별도 존재")
    public ResponseDto<?> getClusters(
            @UserId Long userId
    ) {
        return ResponseDto.ok(clusterService.getClusters(userId));
    }
    @GetMapping("/{cluster_id}/nodes")
    @Operation(summary = "클러스터 노드 목록 조회", description = "등록된 클러스터의 노드 목록을 조회")
    public ResponseDto<?> getClusterNodes(
            @UserId Long userId,
            @PathVariable(name = "cluster_id") Long clusterId
    ) throws Exception {
        return ResponseDto.ok(clusterService.getClusterNodeList(userId, clusterId));
    }


    @GetMapping("/{cluster_id}/nodes/{node_name}")
    @Operation(summary = "클러스터 노드 상세 조회", description = "등록된 클러스터의 노드 상세 조회")
    public ResponseDto<?> getClusterNode(
            @UserId Long userId,
            @PathVariable("cluster_id") Long clusterId,
            @PathVariable("node_name") String nodeName
    ) throws Exception {
        return ResponseDto.ok(clusterService.getClusterNodeDetail(userId, clusterId, nodeName));
    }

    @GetMapping("/{cluster_id}/usage")
    @Operation(summary = "cluster 사용량 ", description = "등록된 클러스터 사용량 조회")
    public ResponseDto<?> getClusterUsage(
            @UserId Long userId,
            @PathVariable("cluster_id") Long clusterId
    ) throws Exception {
        return ResponseDto.ok(clusterService.getClusterUsage(userId, clusterId));
    }

    @GetMapping("/{cluster_id}/usage/{node_name}")
    @Operation(summary = "Cluster-node 사용량 조회", description = "사용자 클러스터-노드 사용량 조회")
    public ResponseDto<?> getClusterNodeSimpleStatus(
            @UserId Long userId,
            @PathVariable(name = "cluster_id") Long clusterId,
            @PathVariable(name = "node_name") String nodeName
    ) throws Exception {
        return ResponseDto.ok(clusterService.getClusterNodeSimpleStatus(userId, clusterId, nodeName));
    }

    @DeleteMapping("/{cluster_id}")
    @Operation(summary = "클러스터 삭제", description = "등록된 클러스터 삭제")
    public ResponseDto<?> deleteCluster(
            @UserId Long userId,
            @PathVariable("cluster_id") Long clusterId
    ) throws Exception {
        clusterService.deleteCluster(userId, clusterId);
        return ResponseDto.ok(null);
    }
}
