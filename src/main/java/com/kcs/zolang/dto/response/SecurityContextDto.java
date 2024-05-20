package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1SecurityContext;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "SecurityContextDto", description = "보안 컨텍스트 Dto")
public record SecurityContextDto(
    Long runAsUser,
    List<String> addedCapabilities,
    List<String> dropCapabilities,
    Boolean allowPrivilegeEscalation,
    Boolean privileged,
    String procMount,
    Boolean readOnlyRootFilesystem,
    Long runAsGroup,
    Boolean runAsNonRoot,
    String seccompProfile,
    String windowsOptions
) {

    public static SecurityContextDto fromEntity(V1SecurityContext securityContext) {
        return SecurityContextDto.builder()
            .runAsUser(securityContext.getRunAsUser())
            .addedCapabilities(securityContext.getCapabilities() == null ? null
                : securityContext.getCapabilities().getAdd())
            .dropCapabilities(securityContext.getCapabilities() == null ? null
                : securityContext.getCapabilities().getDrop())
            .allowPrivilegeEscalation(securityContext.getAllowPrivilegeEscalation() == null ? null
                : securityContext.getAllowPrivilegeEscalation())
            .privileged(
                securityContext.getPrivileged() == null ? null : securityContext.getPrivileged())
            .procMount(
                securityContext.getProcMount() == null ? null : securityContext.getProcMount())
            .readOnlyRootFilesystem(securityContext.getReadOnlyRootFilesystem() == null ? null
                : securityContext.getReadOnlyRootFilesystem())
            .runAsGroup(
                securityContext.getRunAsGroup() == null ? null : securityContext.getRunAsGroup())
            .runAsNonRoot(securityContext.getRunAsNonRoot() == null ? null
                : securityContext.getRunAsNonRoot())
            .seccompProfile(securityContext.getSeccompProfile() == null ? null
                : securityContext.getSeccompProfile().getLocalhostProfile())
            .windowsOptions(securityContext.getWindowsOptions() == null ? null
                : securityContext.getWindowsOptions().getGmsaCredentialSpec())
            .build();
    }
}
