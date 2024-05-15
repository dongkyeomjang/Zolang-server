package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.dto.response.PodSimpleDto;
import com.kcs.zolang.service.WorkloadService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workloads")
@Validated
public class WorkloadController {

    private final WorkloadService podService;

    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/{clusterId}")
    @Operation(summary = "workload overview 조회", description = "모든 네임스페이스의 workload overview 조회")
    public ResponseDto<?> getOverview(
        @UserId Long userId,
        @PathVariable(name = "clusterId") Long clusterId
    ) {
        return ResponseDto.ok(podService.getOverview(userId, clusterId));
    }

    @GetMapping("/{clusterId}/namespace")
    @Operation(summary = "특정 네임스페이스의 workload overview 조회", description = "특정 네임스페이스의 workload overview 조회")
    public ResponseDto<?> getNamespaceOverview(
        @UserId Long userId,
        @NotBlank(message = "namespaced은 공백이 될 수 없습니다.")
        @RequestParam(name = "namespace")
        String namespace,
        @PathVariable(name = "clusterId")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getNameSpaceOverview(userId, namespace, clusterId));
    }

    @GetMapping("/{clusterId}/pods")
    @Operation(summary = "Pod 목록 조회", description = "모든 네임스페이스 Pod 목록 조회")
    public ResponseDto<List<PodSimpleDto>> getPods(
        @UserId Long userId,
        @PathVariable(name = "clusterId")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getPodList(userId, clusterId));
    }

    @GetMapping("/{clusterId}/pods/namespace")
    @Operation(summary = "특정 네임스페이스 Pod 목록 조회", description = "특정 네임스페이스 Pod 목록 조회")
    public ResponseDto<List<PodSimpleDto>> getPodsByNamespace(
        @UserId Long userId,
        @RequestParam(name = "namespace")
        @NotBlank(message = "namespace은 공백이 될 수 없습니다.")
        String namespace,
        @PathVariable(name = "clusterId")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getPodListByNamespace(userId, namespace, clusterId));
    }

    @GetMapping("/{clusterId}/pods/{podName}")
    @Operation(summary = "Pod 상세 조회", description = "특정 Pod 상세 조회")
    public ResponseDto<?> getPod(
        @UserId Long userId,
        @PathVariable(name = "podName") String name,
        @RequestParam(name = "namespace")
        String namespace,
        @PathVariable(name = "clusterId")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getPodDetail(userId, name, namespace, clusterId));
    }

    @GetMapping("/{clusterId}/deployments")
    @Operation(summary = "Deployment 목록 조회", description = "모든 네임스페이스 Deployment 목록 조회")
    public ResponseDto<?> getDeployments(
        @UserId Long userId,
        @PathVariable(name = "clusterId")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getDeploymentList(userId, clusterId));
    }

    @GetMapping("/{clusterId}/daemons")
    @Operation(summary = "DaemonSet 목록 조회", description = "모든 네임스페이스 DaemonSet 목록 조회")
    public ResponseDto<?> getDaemonSets(
        @UserId Long userId,
        @PathVariable(name = "clusterId")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getDaemonSetList(userId, clusterId));
    }

    @GetMapping("/{clusterId}/replicas")
    @Operation(summary = "ReplicaSet 목록 조회", description = "모든 네임스페이스 ReplicaSet 목록 조회")
    public ResponseDto<?> getReplicaSets(
        @UserId Long userId,
        @PathVariable(name = "clusterId")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getReplicaSetList(userId, clusterId));
    }

    @GetMapping("/{clusterId}/statefuls")
    @Operation(summary = "StatefulSet 목록 조회", description = "모든 네임스페이스 StatefulSet 목록 조회")
    public ResponseDto<?> getStatefulSets(
        @UserId Long userId,
        @PathVariable(name = "clusterId")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getStatefulSetList(userId, clusterId));
    }

    @GetMapping("/{clusterId}/cron-jobs")
    @Operation(summary = "CronJob 목록 조회", description = "모든 네임스페이스 CronJob 목록 조회")
    public ResponseDto<?> getCronJobs(
        @UserId Long userId,
        @PathVariable(name = "clusterId")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getCronJobList(userId, clusterId));
    }

    @GetMapping("/{clusterId}/jobs")
    @Operation(summary = "Job 목록 조회", description = "모든 네임스페이스 Job 목록 조회")
    public ResponseDto<?> getJobs(
        @UserId Long userId,
        @PathVariable(name = "clusterId")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getJobList(userId, clusterId));
    }

    @GetMapping("test")
    public String test() {
        redisTemplate.opsForValue().set("cluster-matric:${clusterId}:${mm}", "매트릭 정보 object");
        redisTemplate.expire("cluster-matric:${clusterId}:${mm}", 14, TimeUnit.MINUTES);
        // 1번 클러스터 21분의 데이터 가져오기
        redisTemplate.opsForValue().get("cluster-matric:1:21");

        // 1번 클러스터 21분과 22분 데이터 가져오기 여러개 가져올땐 무조건 이게 권장됨
        redisTemplate.opsForValue().multiGet(List.of("cluster-matric:1:21", "cluster-matric:1:22"));
        return "test";
    }
}
