package com.kcs.zolang.dto.response;

import static com.kcs.zolang.utility.MonitoringUtil.getAge;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Pod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.Builder;

@Builder
@Schema(name = "CommonMetadataDto", description = "Pod 메타데이터 Dto")
public record CommonMetadataDto(
    @Schema(description = "이름", example = "name-123")
    String name,
    @Schema(description = "네임스페이스", example = "default")
    String namespace,
    @Schema(description = "생성 날짜", example = "2024. 01. 01.")
    String creationDate,
    @Schema(description = "생성 시간", example = "AM 10:00")
    String creationTime,
    @Schema(description = "실행 시간", example = "1d")
    String age,
    @Schema(description = "UID", example = "pod-uid")
    String uid,
    @Schema(description = "레이블", example = "k8ss-app:kube-dns")
    Map<String, String> labels,
    @Schema(description = "어노테이션", example = "kubectl.kubernetes.io/restartedAt: 2024-05-10T14:13:31Z")
    Map<String, String> annotations) {

    public static CommonMetadataDto fromEntity(V1Pod pod) {
        return CommonMetadataDto.builder()
            .name(pod.getMetadata().getName())
            .namespace(pod.getMetadata().getNamespace())
            .creationDate(pod.getMetadata().getCreationTimestamp().toLocalDate()
                .format(DateTimeFormatter.ofPattern("yyyy .MM .dd .")))
            .creationTime(pod.getMetadata().getCreationTimestamp().toLocalTime()
                .format(DateTimeFormatter.ofPattern("a hh:mm:ss")))
            .age(getAge(pod.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .uid(pod.getMetadata().getUid())
            .labels(pod.getMetadata().getLabels())
            .annotations(pod.getMetadata().getAnnotations())
            .build();
    }

    public static CommonMetadataDto fromEntity(V1Deployment deployment) {
        return CommonMetadataDto.builder()
            .name(deployment.getMetadata().getName())
            .namespace(deployment.getMetadata().getNamespace())
            .creationDate(deployment.getMetadata().getCreationTimestamp().toLocalDate()
                .format(DateTimeFormatter.ofPattern("yyyy .MM .dd .")))
            .creationTime(deployment.getMetadata().getCreationTimestamp().toLocalTime()
                .format(DateTimeFormatter.ofPattern("a hh:mm:ss")))
            .age(getAge(deployment.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .uid(deployment.getMetadata().getUid())
            .labels(deployment.getMetadata().getLabels())
            .annotations(deployment.getMetadata().getAnnotations())
            .build();
    }
}
