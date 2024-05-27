package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.service.NetworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cluster")
@Validated
@Tag(name="Monitoring-Network API", description="모니터링-네트워크 API")
public class NetworkController {
    private final NetworkService networkService;

    @GetMapping("/{cluster_id}/network/service")
    @Operation(summary = "Network-service 목록 조회", description = "사용자의 네트워크-서비스 목록 조회")
    public ResponseDto<?> getServiceList(
            @UserId Long userId,
            @PathVariable(name = "cluster_id") Long clusterId
    ) {
        return ResponseDto.ok(networkService.getServiceList(userId, clusterId));
    }

    @GetMapping("/{cluster_id}/network/service/namespace")
    @Operation(summary = "특정 카테고리 Network-service 목록 조회", description = "사용자의 네트워크-서비스 특정 네임스페이스 목록 조회")
    public ResponseDto<?> getServiceNameList(
            @UserId Long userId,
            @RequestParam(name="namespace") String namespace,
            @PathVariable(name = "cluster_id") Long clusterId
    ) {
        return ResponseDto.ok(networkService.getServiceNameList(userId, clusterId, namespace));
    }

    @GetMapping("/{cluster_id}/network/service/{service_name}")
    @Operation(summary = "Network-service 상세 조회", description = "사용자의 네트워크-서비스 상세 조회")
    public ResponseDto<?> getServiceDetail(
            @UserId Long userId,
            @PathVariable(name = "cluster_id") Long clusterId,
            @PathVariable(name = "service_name") String serviceName
    ) {
        return ResponseDto.ok(networkService.getServiceDetail(userId, clusterId, serviceName));
    }

}
