package com.kcs.zolang.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "클러스터 등록 요청 Dto")
public record RegisterClusterDto(
        @JsonProperty("cluster_name") @Schema(description = "클러스터 이름", required = true)
        String clusterName,
        @JsonProperty("domain_url") @Schema(description = "클러스터 도메인 URL", required = true)
        String domainUrl,
        @JsonProperty("secret_token") @Schema(description = "클러스터에 대한 접근 권한을 받은 SA에 대한 시크릿 토큰", required = true)
        String secretToken,
        @JsonProperty("version") @Schema(description = "클러스터 버전", required = true)
        String version
) {
}
