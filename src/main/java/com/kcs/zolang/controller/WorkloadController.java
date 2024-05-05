package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.dto.response.PodSimpleDto;
import com.kcs.zolang.service.WorkloadService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workloads")
@Validated
public class WorkloadController {

    private final WorkloadService podService;

    @GetMapping
    @Operation(summary = "workload overview 조회", description = "모든 네임스페이스의 workload overview 조회")
    public ResponseDto<?> getOverview(
        @UserId Long userId
    ) {
        return ResponseDto.ok(podService.getOverview(userId));
    }

    @GetMapping("/namespace")
    @Operation(summary = "특정 네임스페이스의 workload overview 조회", description = "특정 네임스페이스의 workload overview 조회")
    public ResponseDto<?> getNamespaceOverview(
        @UserId Long userId,
        @NotBlank(message = "namespaced은 공백이 될 수 없습니다.")
        @RequestParam(name = "namespace")
        String namespace
    ) {
        return ResponseDto.ok(podService.getNameSpaceOverview(userId, namespace));
    }

    @GetMapping("/pods")
    @Operation(summary = "Pod 목록 조회", description = "모든 네임스페이스 Pod 목록 조회")
    public ResponseDto<List<PodSimpleDto>> getPods(
        @UserId Long userId
    ) {
        return ResponseDto.ok(podService.getPodList(userId));
    }

    @GetMapping("/pods/namespace")
    @Operation(summary = "특정 네임스페이스 Pod 목록 조회", description = "특정 네임스페이스 Pod 목록 조회")
    public ResponseDto<List<PodSimpleDto>> getPodsByNamespace(
        @UserId Long userId,
        @RequestParam(name = "namespace")
        @NotBlank(message = "namespace은 공백이 될 수 없습니다.")
        String namespace
    ) {
        return ResponseDto.ok(podService.getPodListByNamespace(userId, namespace));
    }
}
