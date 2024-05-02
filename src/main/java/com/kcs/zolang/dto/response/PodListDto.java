package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1PodList;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;

@Builder
public record PodListDto(
    List<PodDto> items
) implements Serializable {
    public static PodListDto of(V1PodList podList) {
        return PodListDto.builder()
            .items(podList.getItems().stream().map(PodDto::fromEntity).toList())
            .build();
    }
}
