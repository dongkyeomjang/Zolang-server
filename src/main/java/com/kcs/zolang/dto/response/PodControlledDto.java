package com.kcs.zolang.dto.response;

import static com.kcs.zolang.utility.MonitoringUtil.getAge;

import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
@Schema(name = "PodControlledDto", description = "Pod 컨트롤러 Dto")
public record PodControlledDto(
    @Schema(description = "컨트롤러 이름")
    String name,
    @Schema(description = "컨트롤러 종류")
    String kind,
    @Schema(description = "파드 수")
    int replicas,
    @Schema(description = "실행 중인 파드 수")
    int readyReplicas,
    @Schema(description = "생성 시간")
    String age,
    @Schema(description = "레이블 목록")
    Map<String, String> labels,
    @Schema(description = "이미지 목록")
    List<String> images
) {

    public static PodControlledDto fromEntity(V1Deployment deployment) {
        return PodControlledDto.builder()
            .name(deployment.getMetadata().getName())
            .kind(deployment.getKind())
            .replicas(deployment.getStatus().getReplicas())
            .readyReplicas(deployment.getStatus().getReadyReplicas())
            .age(getAge(
                deployment.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .images(deployment.getSpec().getTemplate().getSpec().getContainers().stream()
                .map(it -> it.getImage()).toList())
            .labels(deployment.getMetadata().getLabels())
            .build();
    }

    public static PodControlledDto fromEntity(V1DaemonSet daemonSet) {
        return PodControlledDto.builder()
            .name(daemonSet.getMetadata().getName())
            .kind(daemonSet.getKind())
            .replicas(daemonSet.getStatus().getNumberAvailable())
            .readyReplicas(daemonSet.getStatus().getNumberReady())
            .age(getAge(
                daemonSet.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .images(daemonSet.getSpec().getTemplate().getSpec().getContainers().stream()
                .map(it -> it.getImage()).toList())
            .labels(daemonSet.getMetadata().getLabels())
            .build();
    }

    public static PodControlledDto fromEntity(V1ReplicaSet replicaSet) {
        return PodControlledDto.builder()
            .name(replicaSet.getMetadata().getName())
            .kind(replicaSet.getKind())
            .replicas(replicaSet.getStatus().getReplicas())
            .readyReplicas(replicaSet.getStatus().getReadyReplicas())
            .age(getAge(
                replicaSet.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .images(replicaSet.getSpec().getTemplate().getSpec().getContainers().stream()
                .map(it -> it.getImage()).toList())
            .labels(replicaSet.getMetadata().getLabels())
            .build();
    }

    public static PodControlledDto fromEntity(V1StatefulSet statefulSet) {
        return PodControlledDto.builder()
            .name(statefulSet.getMetadata().getName())
            .kind(statefulSet.getKind())
            .replicas(statefulSet.getStatus().getReplicas())
            .readyReplicas(statefulSet.getStatus().getReadyReplicas())
            .age(getAge(
                statefulSet.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .images(statefulSet.getSpec().getTemplate().getSpec().getContainers().stream()
                .map(it -> it.getImage()).toList())
            .labels(statefulSet.getMetadata().getLabels())
            .build();
    }

    public static PodControlledDto fromEntity(V1CronJob cronJob) {
        return PodControlledDto.builder()
            .name(cronJob.getMetadata().getName())
            .kind(cronJob.getKind())
            .replicas(0)
            .readyReplicas(0)
            .age(getAge(
                cronJob.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .images(
                cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers()
                    .stream()
                    .map(it -> it.getImage()).toList())
            .labels(cronJob.getMetadata().getLabels())
            .build();
    }

    public static PodControlledDto fromEntity(V1Job job) {
        return PodControlledDto.builder()
            .name(job.getMetadata().getName())
            .kind(job.getKind())
            .replicas(0)
            .readyReplicas(0)
            .age(getAge(
                job.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .images(job.getSpec().getTemplate().getSpec().getContainers().stream()
                .map(it -> it.getImage()).toList())
            .labels(job.getMetadata().getLabels())
            .build();
    }
}
