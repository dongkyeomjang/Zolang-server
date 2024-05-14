package com.kcs.zolang.dto.response;

import static com.kcs.zolang.utility.MonitoringUtil.DATE_TIME_FORMATTER;
import static com.kcs.zolang.utility.MonitoringUtil.byteConverter;
import static com.kcs.zolang.utility.MonitoringUtil.getAge;

import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import lombok.Builder;

@Builder
@Schema(name = "PodPersistentVolumeClaimDto", description = "Pod PersistentVolumeClaim Dto")
public record PodPersistentVolumeClaimDto(
    @Schema(description = "PVC 이름", example = "pvc-name")
    String name,
    @Schema(description = "PVC 네임스페이스", example = "default")
    String namespace,
    @Schema(description = "PVC 레이블")
    String label,
    @Schema(description = "PVC 상태", example = "Bound")
    String status,
    @Schema(description = "PVC 볼륨 이름", example = "volume-name")
    String volume,
    @Schema(description = "PVC 용량", example = "1Gi")
    String capacity,
    @Schema(description = "PVC 접근 모드", example = "ReadWriteOnce")
    List<String> accessMode,
    @Schema(description = "PVC 스토리지 클래스", example = "grafana")
    String storageClass,
    @Schema(description = "PVC 생성 시간", example = "1d")
    String age,
    @Schema(description = "PVC 생성 일시", example = "2021. 12. 01. 오후 12:00:00")
    String creationDateTime
) {

    public static PodPersistentVolumeClaimDto fromEntity(
        V1PersistentVolumeClaim persistentVolumeClaim) {
        return PodPersistentVolumeClaimDto.builder()
            .name(Objects.requireNonNull(persistentVolumeClaim.getMetadata()).getName())
            .namespace(persistentVolumeClaim.getMetadata().getNamespace())
            .label(Objects.toString(persistentVolumeClaim.getMetadata().getLabels()))
            .status(persistentVolumeClaim.getStatus().getPhase())
            .volume(Objects.requireNonNull(persistentVolumeClaim.getSpec()).getVolumeName())
            .capacity(byteConverter(
                persistentVolumeClaim.getStatus().getCapacity().get("storage").getNumber()
                    .toString()))
            .accessMode(persistentVolumeClaim.getSpec().getAccessModes())
            .storageClass(persistentVolumeClaim.getSpec().getStorageClassName())
            .age(getAge(
                Objects.requireNonNull(persistentVolumeClaim.getMetadata().getCreationTimestamp())
                    .toLocalDateTime()))
            .creationDateTime(
                persistentVolumeClaim.getMetadata().getCreationTimestamp().toLocalDateTime()
                    .format(DATE_TIME_FORMATTER))
            .build();
    }
}
