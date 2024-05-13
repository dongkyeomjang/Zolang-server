package com.kcs.zolang.dto.response;

import static com.kcs.zolang.utility.MonitoringUtil.DATE_TIME_FORMATTER;
import static com.kcs.zolang.utility.MonitoringUtil.getAge;

import io.kubernetes.client.openapi.models.V1Container;
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
@Schema(name = "CommonControllerDto", description = "공통 컨트롤러 Dto")
public record CommonControllerDto(
    @Schema(description = "컨트롤러 이름")
    String name,
    @Schema(description = "컨트롤러 네임스페이스")
    String namespace,
    @Schema(description = "이미지 목록")
    List<String> images,
    @Schema(description = "레이블 목록")
    Map<String, String> labels,
    @Schema(description = "파드 수")
    int replicas,
    @Schema(description = "실행 중인 파드 수")
    int readyReplicas,
    @Schema(description = "생성 일시")
    String creationDateTime,
    @Schema(description = "생성 시간")
    String age
) {

    public static CommonControllerDto fromEntity(V1Deployment deployment) {
        return CommonControllerDto.builder()
            .name(deployment.getMetadata().getName())
            .namespace(deployment.getMetadata().getNamespace())
            .images(deployment.getSpec().getTemplate().getSpec().getContainers().stream()
                .map(it -> it.getImage()).toList())
            .labels(deployment.getMetadata().getLabels())
            .replicas(deployment.getSpec().getReplicas())
            .readyReplicas(deployment.getStatus().getReadyReplicas())
            .creationDateTime(
                deployment.getMetadata().getCreationTimestamp().toLocalDateTime()
                    .format(DATE_TIME_FORMATTER))
            .age(getAge(
                deployment.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .build();
    }

    public static CommonControllerDto fromEntity(V1DaemonSet daemonSet) {
        return CommonControllerDto.builder()
            .name(daemonSet.getMetadata().getName())
            .namespace(daemonSet.getMetadata().getNamespace())
            .images(daemonSet.getSpec().getTemplate().getSpec().getContainers().stream()
                .map(it -> it.getImage()).toList())
            .labels(daemonSet.getMetadata().getLabels())
            .replicas(daemonSet.getStatus().getNumberAvailable())
            .readyReplicas(daemonSet.getStatus().getNumberReady())
            .creationDateTime(
                daemonSet.getMetadata().getCreationTimestamp().toLocalDateTime()
                    .format(DATE_TIME_FORMATTER))
            .age(getAge(
                daemonSet.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .build();
    }

    public static CommonControllerDto fromEntity(V1ReplicaSet replicaSet) {
        return CommonControllerDto.builder()
            .name(replicaSet.getMetadata().getName())
            .namespace(replicaSet.getMetadata().getNamespace())
            .images(replicaSet.getSpec().getTemplate().getSpec().getContainers().stream()
                .map(V1Container::getImage).toList())
            .labels(replicaSet.getMetadata().getLabels())
            .replicas(replicaSet.getStatus().getReplicas())
            .readyReplicas(
                replicaSet.getStatus().getReadyReplicas() != null ? replicaSet.getStatus()
                    .getReadyReplicas() : 0)
            .creationDateTime(
                replicaSet.getMetadata().getCreationTimestamp().toLocalDateTime().format(
                    DATE_TIME_FORMATTER))
            .age(getAge(
                replicaSet.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .build();
    }

    public static CommonControllerDto fromEntity(V1StatefulSet statefulSet) {
        return CommonControllerDto.builder()
            .name(statefulSet.getMetadata().getName())
            .namespace(statefulSet.getMetadata().getNamespace())
            .images(statefulSet.getSpec().getTemplate().getSpec().getContainers().stream()
                .map(it -> it.getImage()).toList())
            .labels(statefulSet.getMetadata().getLabels())
            .replicas(statefulSet.getSpec().getReplicas())
            .readyReplicas(statefulSet.getStatus().getReadyReplicas())
            .creationDateTime(
                statefulSet.getMetadata().getCreationTimestamp().toLocalDateTime().format(
                    DATE_TIME_FORMATTER))
            .age(getAge(
                statefulSet.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .build();
    }

    public static CommonControllerDto fromEntity(V1Job job) {
        return CommonControllerDto.builder()
            .name(job.getMetadata().getName())
            .namespace(job.getMetadata().getNamespace())
            .images(job.getSpec().getTemplate().getSpec().getContainers().stream()
                .map(it -> it.getImage()).toList())
            .labels(job.getMetadata().getLabels())
            .replicas(job.getSpec().getParallelism())
            .readyReplicas(job.getStatus().getSucceeded())
            .creationDateTime(
                job.getMetadata().getCreationTimestamp().toLocalDateTime().format(
                    DATE_TIME_FORMATTER))
            .age(getAge(
                job.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .build();
    }
}
