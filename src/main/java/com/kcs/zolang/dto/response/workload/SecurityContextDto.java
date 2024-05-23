package com.kcs.zolang.dto.response.workload;

import io.kubernetes.client.openapi.models.V1SecurityContext;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "SecurityContextDto", description = "보안 컨텍스트 Dto")
public record SecurityContextDto(
    @Schema(description = "사용자 ID", example = "1000")
    Long runAsUser,
    @Schema(description = "추가 가능한 능력", example = "add-capability")
    List<String> addedCapabilities,
    @Schema(description = "제거 가능한 능력", example = "drop-capability")
    List<String> dropCapabilities,
    @Schema(description = "특권 상승 허용 여부", example = "true")
    Boolean allowPrivilegeEscalation,
    @Schema(description = "특권 여부", example = "true")
    Boolean privileged,
    @Schema(description = "프로세스 마운트", example = "proc-mount")
    String procMount,
    @Schema(description = "루트 파일 시스템 읽기 전용 여부", example = "true")
    Boolean readOnlyRootFilesystem,
    @Schema(description = "그룹 ID", example = "1000")
    Long runAsGroup,
    @Schema(description = "루트가 아닌 사용자로 실행 여부", example = "true")
    Boolean runAsNonRoot,
    @Schema(description = "Seccomp 프로필", example = "seccomp-profile")
    String seccompProfile,
    @Schema(description = "Windows 옵션", example = "gmsa-credential-spec")
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
