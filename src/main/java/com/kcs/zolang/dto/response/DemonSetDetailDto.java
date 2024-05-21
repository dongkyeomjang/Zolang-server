package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "DemonSetDetailDto", description = "데몬셋 상세 Dto")
public record DemonSetDetailDto(
    @Schema(description = "데몬셋 메타데이터 Dto")
    CommonMetadataDto commonControllerDto,
    @Schema(description = "데몬셋 리소스 Dto")
    DemonSetResourceDto demonSetResourceDto,
    @Schema(description = "pod 상태")
    DemonSetPodStatusDto podConditions,
    @Schema(description = "해당 데몬셋 파드 정보")
    List<PodSimpleDto> pods,
    @Schema(description = "해당 데몬셋 서비스 정보")
    List<ServiceSimpleDto> services
) {

    public static DemonSetDetailDto fromEntity(V1DaemonSet daemonSet, List<PodSimpleDto> pods,
        List<ServiceSimpleDto> services) {
        return DemonSetDetailDto.builder()
            .commonControllerDto(daemonSet.getMetadata() == null ? null
                : CommonMetadataDto.fromEntity(daemonSet.getMetadata()))
            .demonSetResourceDto(DemonSetResourceDto.fromEntity(daemonSet))
            .podConditions(DemonSetPodStatusDto.fromEntity(daemonSet))
            .pods(pods)
            .services(services)
            .build();
    }
}
