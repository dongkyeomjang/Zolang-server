package com.kcs.zolang.dto.response.workload;

import com.kcs.zolang.dto.response.network.ServiceListDto;
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
    CommonMetadataDto metadata,
    @Schema(description = "컨트롤러 리소스 Dto")
    CommonControllerResourceDto resource,
    @Schema(description = "pod 상태")
    CommonControllerPodStatusDto podConditions,
    @Schema(description = "해당 컨트롤러 파드 정보")
    List<PodSimpleDto> pods,
    @Schema(description = "해당 컨트롤러 서비스 정보")
    List<ServiceListDto> services
) {

    public static CommonControllerDetailDto fromEntity(V1DaemonSet daemonSet,
        List<PodSimpleDto> pods,
        List<ServiceListDto> services) {
        return CommonControllerDetailDto.builder()
            .metadata(daemonSet.getMetadata() == null ? null
                : CommonMetadataDto.fromEntity(daemonSet.getMetadata()))
            .resource(CommonControllerResourceDto.fromEntity(daemonSet))
            .podConditions(CommonControllerPodStatusDto.fromEntity(daemonSet))
            .pods(pods)
            .services(services)
            .build();
    }

    public static CommonControllerDetailDto fromEntity(V1StatefulSet statefulSet,
        List<PodSimpleDto> pods) {
        return CommonControllerDetailDto.builder()
            .metadata(statefulSet.getMetadata() == null ? null
                : CommonMetadataDto.fromEntity(statefulSet.getMetadata()))
            .resource(CommonControllerResourceDto.fromEntity(statefulSet))
            .podConditions(CommonControllerPodStatusDto.fromEntity(statefulSet))
            .pods(pods)
            .build();
    }

    public static CommonControllerDetailDto fromEntity(V1ReplicaSet replicaSet,
        List<PodSimpleDto> pods, List<ServiceListDto> services) {
        return CommonControllerDetailDto.builder()
            .metadata(replicaSet.getMetadata() == null ? null
                : CommonMetadataDto.fromEntity(replicaSet.getMetadata()))
            .resource(CommonControllerResourceDto.fromEntity(replicaSet))
            .podConditions(CommonControllerPodStatusDto.fromEntity(replicaSet))
            .pods(pods)
            .services(services)
            .build();
    }
}
