package com.kcs.zolang.dto.response;

import com.kcs.zolang.domain.Cluster;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Schema(name = "UserUrlTokenDto", description = "유저 URL, Token Dto")
@Builder
public record UserUrlTokenDto(
    @NotEmpty(message = "URL이 없습니다.")
    String url,
    @NotEmpty (message = "Token이 없습니다.")
    String token,
    @NotEmpty (message = "caCert가 없습니다.")
    String caCert
) {
    public static UserUrlTokenDto fromEntity(Cluster cluster) {
        return UserUrlTokenDto.builder()
            .url(cluster.getDomainUrl())
            .token(cluster.getSecretToken())
            .caCert(cluster.getCertPath()).build();
    }
}
