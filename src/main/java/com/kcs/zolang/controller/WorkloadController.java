package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.dto.response.workload.PodListDto;
import com.kcs.zolang.service.WorkloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cluster")
@Validated
@Tag(name = "Monitoring-Workload", description = "Workload 모니터링 관련 API")
public class WorkloadController {

    private final WorkloadService podService;

    @GetMapping("/{cluster_id}/workload/overview")
    @Operation(summary = "workload overview 조회", description = "모든 네임스페이스의 workload overview 조회")
    public ResponseDto<?> getOverview(
        @UserId Long userId,
        @PathVariable(name = "cluster_id") Long clusterId
    ) {
        return ResponseDto.ok(podService.getOverview(userId, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/overview/namespace")
    @Operation(summary = "특정 네임스페이스의 workload overview 조회", description = "특정 네임스페이스의 workload overview 조회")
    public ResponseDto<?> getNamespaceOverview(
        @UserId Long userId,
        @NotBlank(message = "namespaced은 공백이 될 수 없습니다.")
        @RequestParam(name = "namespace")
        String namespace,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getNameSpaceOverview(userId, namespace, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/pods")
    @Operation(summary = "Pod 목록 조회", description = "모든 네임스페이스 Pod 목록 조회")
    public ResponseDto<PodListDto> getPods(
        @UserId Long userId,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getPodList(userId, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/pods/namespace")
    @Operation(summary = "특정 네임스페이스 Pod 목록 조회", description = "특정 네임스페이스 Pod 목록 조회")
    public ResponseDto<PodListDto> getPodsByNamespace(
        @UserId Long userId,
        @RequestParam(name = "namespace")
        @NotBlank(message = "namespace은 공백이 될 수 없습니다.")
        String namespace,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getPodListByNamespace(userId, namespace, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/pods/{pod_name}")
    @Operation(summary = "Pod 상세 조회", description = "특정 Pod 상세 조회")
    public ResponseDto<?> getPod(
        @UserId Long userId,
        @PathVariable(name = "podName") String name,
        @RequestParam(name = "namespace")
        String namespace,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getPodDetail(userId, name, namespace, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/deployments")
    @Operation(summary = "Deployment 목록 조회", description = "모든 네임스페이스 Deployment 목록 조회")
    public ResponseDto<?> getDeployments(
        @UserId Long userId,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getDeploymentList(userId, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/deployments/{deployment_name}")
    @Operation(summary = "Deployment 상세 조회", description = "특정 Deployment 상세 조회")
    public ResponseDto<?> getDeploymentDetail(
        @UserId Long userId,
        @PathVariable(name = "deploymentName") String name,
        @RequestParam(name = "namespace")
        String namespace,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getDeploymentDetail(userId, name, namespace, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/deployments/namespace")
    @Operation(summary = "특정 네임스페이스 Deployment 목록 조회", description = "특정 네임스페이스 Deployment 목록 조회")
    public ResponseDto<?> getDeploymentsByNamespace(
        @UserId Long userId,
        @RequestParam(name = "namespace")
        @NotBlank(message = "namespace은 공백이 될 수 없습니다.")
        String namespace,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(
            podService.getDeploymentListByNamespace(userId, namespace, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/daemons")
    @Operation(summary = "DaemonSet 목록 조회", description = "모든 네임스페이스 DaemonSet 목록 조회")
    public ResponseDto<?> getDaemonSets(
        @UserId Long userId,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getDaemonSetList(userId, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/daemons/namespace")
    @Operation(summary = "특정 네임스페이스 DaemonSet 목록 조회", description = "특정 네임스페이스 DaemonSet 목록 조회")
    public ResponseDto<?> getDaemonSetsByNamespace(
        @UserId Long userId,
        @RequestParam(name = "namespace")
        @NotBlank(message = "namespace은 공백이 될 수 없습니다.")
        String namespace,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getDaemonSetListByNamespace(userId, namespace, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/daemons/{daemon_set_name}")
    @Operation(summary = "DaemonSet 상세 조회", description = "특정 DaemonSet 상세 조회")
    public ResponseDto<?> getDaemonSetDetail(
        @UserId Long userId,
        @PathVariable(name = "daemonSetName") String name,
        @RequestParam(name = "namespace")
        String namespace,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getDaemonSetDetail(userId, name, namespace, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/replicas")
    @Operation(summary = "ReplicaSet 목록 조회", description = "모든 네임스페이스 ReplicaSet 목록 조회")
    public ResponseDto<?> getReplicaSets(
        @UserId Long userId,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getReplicaSetList(userId, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/replicas/namespace")
    @Operation(summary = "특정 네임스페이스 ReplicaSet 목록 조회", description = "특정 네임스페이스 ReplicaSet 목록 조회")
    public ResponseDto<?> getReplicaSetsByNamespace(
        @UserId Long userId,
        @RequestParam(name = "namespace")
        @NotBlank(message = "namespace은 공백이 될 수 없습니다.")
        String namespace,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(
            podService.getReplicaSetListByNamespace(userId, namespace, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/replicas/{replica_set_name}")
    @Operation(summary = "ReplicaSet 상세 조회", description = "특정 ReplicaSet 상세 조회")
    public ResponseDto<?> getReplicaSetDetail(
        @UserId Long userId,
        @PathVariable(name = "replicaSetName") String name,
        @RequestParam(name = "namespace")
        String namespace,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getReplicaSetDetail(userId, name, namespace, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/statefuls")
    @Operation(summary = "StatefulSet 목록 조회", description = "모든 네임스페이스 StatefulSet 목록 조회")
    public ResponseDto<?> getStatefulSets(
        @UserId Long userId,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getStatefulSetList(userId, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/statefuls/namespace")
    @Operation(summary = "특정 네임스페이스 StatefulSet 목록 조회", description = "특정 네임스페이스 StatefulSet 목록 조회")
    public ResponseDto<?> getStatefulSetsByNamespace(
        @UserId Long userId,
        @RequestParam(name = "namespace")
        @NotBlank(message = "namespace은 공백이 될 수 없습니다.")
        String namespace,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(
            podService.getStatefulSetListByNamespace(userId, namespace, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/statefuls/{stateful_set_name}")
    @Operation(summary = "StatefulSet 상세 조회", description = "특정 StatefulSet 상세 조회")
    public ResponseDto<?> getStatefulSetDetail(
        @UserId Long userId,
        @PathVariable(name = "statefulSetName") String name,
        @RequestParam(name = "namespace")
        String namespace,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getStatefulSetDetail(userId, name, namespace, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/cron-jobs")
    @Operation(summary = "CronJob 목록 조회", description = "모든 네임스페이스 CronJob 목록 조회")
    public ResponseDto<?> getCronJobs(
        @UserId Long userId,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getCronJobList(userId, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/cron-jobs/namespace")
    @Operation(summary = "특정 네임스페이스 CronJob 목록 조회", description = "특정 네임스페이스 CronJob 목록 조회")
    public ResponseDto<?> getCronJobsByNamespace(
        @UserId Long userId,
        @RequestParam(name = "namespace")
        @NotBlank(message = "namespace은 공백이 될 수 없습니다.")
        String namespace,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getCronJobListByNamespace(userId, namespace, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/jobs")
    @Operation(summary = "Job 목록 조회", description = "모든 네임스페이스 Job 목록 조회")
    public ResponseDto<?> getJobs(
        @UserId Long userId,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getJobList(userId, clusterId));
    }

    @GetMapping("/{cluster_id}/workload/jobs/namespace")
    @Operation(summary = "특정 네임스페이스 Job 목록 조회", description = "특정 네임스페이스 Job 목록 조회")
    public ResponseDto<?> getJobsByNamespace(
        @UserId Long userId,
        @RequestParam(name = "namespace")
        @NotBlank(message = "namespace은 공백이 될 수 없습니다.")
        String namespace,
        @PathVariable(name = "cluster_id")
        Long clusterId
    ) {
        return ResponseDto.ok(podService.getJobListByNamespace(userId, namespace, clusterId));
    }
}
