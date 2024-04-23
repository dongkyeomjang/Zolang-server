package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.service.ClusterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cluster")
@Tag(name = "Monitoring", description = "모니터링 관련 API")
public class ClusterController {
    private final ClusterService clusterService;
    @GetMapping("")
    @Operation(summary = "클러스터 목록 조회", description = "사용자의 클러스터 목록을 조회. 단, DB에 있는 값만 들고옴. 상태조회 API 별도 존재")
    public ResponseDto<?> getClusterList(
            @UserId Long userId
    ) {
        return ResponseDto.ok(clusterService.getClusterList(userId));
    }
    @GetMapping("/{clusterId}/status")
    @Operation(summary = "클러스터 상태 조회", description = "클러스터의 상태를 조회. 클러스터 목록 조회와 같이 호출되어야함")
    public ResponseDto<?> getClusterStatus(
            @PathVariable Long clusterId
    ){
        return ResponseDto.ok(clusterService.getClusterStatus(clusterId));
    }
}
