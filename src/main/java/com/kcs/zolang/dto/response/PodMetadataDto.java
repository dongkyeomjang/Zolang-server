package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
@Schema(name = "PodMetadataDto", description = "Pod 메타데이터 Dto")
public record PodMetadataDto(
    @Schema(description = "Pod 이름", example = "pod-name")
    String name,
    @Schema(description = "Pod 네임스페이스", example = "default")
    String namespace,
    @Schema(description = "Conditions", example = "ready")
    List<String> conditions,
    @Schema(description = "Pod 생성 시간", example = "2024.01.01")
    String creationTime,
    @Schema(description = "Pod 실행 시간", example = "1d")
    String age,
    @Schema(description = "Pod UID", example = "pod-uid")
    String uid,
    @Schema(description = "Pod 레이블", example = "k8ss-app:kube-dns")
    Map<String, String> labels,
    @Schema(description = "Pod 어노테이션", example = "kubectl.kubernetes.io/restartedAt: 2024-05-10T14:13:31Z")
    Map<String, String> annotations) {

    public static PodMetadataDto fromEntity(V1Pod pod, String age) {
        return PodMetadataDto.builder()
            .name(pod.getMetadata().getName())
            .namespace(pod.getMetadata().getNamespace())
            .conditions(
                pod.getStatus().getConditions().stream().map(V1PodCondition::getType)
                    .toList())
            .creationTime(pod.getMetadata().getCreationTimestamp().toLocalDate()
                .format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
            .age(age)
            .uid(pod.getMetadata().getUid())
            .labels(pod.getMetadata().getLabels())
            .annotations(pod.getMetadata().getAnnotations())
            .build();
    }

}
