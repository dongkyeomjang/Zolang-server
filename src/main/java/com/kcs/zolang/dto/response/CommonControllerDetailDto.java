package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "CommonControllerDetailDto", description = "컨트롤러 상세 Dto")
public record CommonControllerDetailDto(
    @Schema(description = "컨트롤러 메타데이터 Dto")
    CommonMetadataDto commonControllerDto,
    @Schema(description = "컨트롤러 리소스 Dto")
    CommonControllerResourceDto commonControllerResourceDto,
    @Schema(description = "pod 상태")
    CommonControllerPodStatusDto podConditions,
    @Schema(description = "해당 컨트롤러 파드 정보")
    List<PodSimpleDto> pods,
    @Schema(description = "해당 컨트롤러 서비스 정보")
    List<ServiceSimpleDto> services
) {

    public static CommonControllerDetailDto fromEntity(V1DaemonSet daemonSet,
        List<PodSimpleDto> pods,
        List<ServiceSimpleDto> services) {
        return CommonControllerDetailDto.builder()
            .commonControllerDto(daemonSet.getMetadata() == null ? null
                : CommonMetadataDto.fromEntity(daemonSet.getMetadata()))
            .commonControllerResourceDto(CommonControllerResourceDto.fromEntity(daemonSet))
            .podConditions(CommonControllerPodStatusDto.fromEntity(daemonSet))
            .pods(pods)
            .services(services)
            .build();
    }

    public static CommonControllerDetailDto fromEntity(V1StatefulSet statefulSet,
        List<PodSimpleDto> pods) {
        return CommonControllerDetailDto.builder()
            .commonControllerDto(statefulSet.getMetadata() == null ? null
                : CommonMetadataDto.fromEntity(statefulSet.getMetadata()))
            .commonControllerResourceDto(CommonControllerResourceDto.fromEntity(statefulSet))
            .podConditions(CommonControllerPodStatusDto.fromEntity(statefulSet))
            .pods(pods)
            .services(null)
            .build();
    }

    public static CommonControllerDetailDto fromEntity(V1ReplicaSet replicaSet,
        List<PodSimpleDto> pods, List<ServiceSimpleDto> services) {
        return CommonControllerDetailDto.builder()
            .commonControllerDto(replicaSet.getMetadata() == null ? null
                : CommonMetadataDto.fromEntity(replicaSet.getMetadata()))
            .commonControllerResourceDto(CommonControllerResourceDto.fromEntity(replicaSet))
            .podConditions(CommonControllerPodStatusDto.fromEntity(replicaSet))
            .pods(pods)
            .services(services)
            .build();
    }
}
