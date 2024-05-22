package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.service.NetworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/network")
@Validated
@Tag(name="Monitoring-Network API", description="모니터링-네트워크 API")
public class NetworkController {
    private final NetworkService networkService;

    @GetMapping("/{clusterId}/service")
    @Operation(summary = "Network-service 목록 조회", description = "사용자의 네트워크-서비스 목록 조회")
    public ResponseDto<?> getServiceList(
            @UserId Long userId,
            @PathVariable(name = "clusterId") Long clusterId
    ) {
        return ResponseDto.ok(networkService.getServiceList(userId, clusterId));
    }

    @GetMapping("/{clusterId}/service/{serviceName}")
    @Operation(summary = "Network-service 목록 조회", description = "사용자의 네트워크-서비스 상세 조회")
    public ResponseDto<?> getServiceDetail(
            @UserId Long userId,
            @PathVariable(name = "clusterId") Long clusterId,
            @PathVariable(name = "serviceName") String serviceName
    ) {
        return ResponseDto.ok(networkService.getServiceDetail(userId, clusterId, serviceName));
    }

}
