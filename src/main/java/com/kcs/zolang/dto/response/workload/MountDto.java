package com.kcs.zolang.dto.response.workload;

import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "MountDto", description = "마운트 정보 Dto")
public record MountDto(
    @Schema(description = "마운트 이름", example = "mount-name")
    String name,
    @Schema(description = "읽기 전용 여부", example = "true")
    boolean readOnly,
    @Schema(description = "마운트 경로", example = "/path")
    String mountPath,
    @Schema(description = "서브 경로", example = "/sub-path")
    String subPath,
    @Schema(description = "소스 타입", example = "source-type")
    String sourceType,
    @Schema(description = "소스 이름", example = "source-name")
    String sourceName
) {

    public static MountDto fromEntity(V1VolumeMount volumeMounts, List<V1Volume> volumes) {
        V1Volume volume;
        String sourceType = null;
        String sourceName = null;
        for (V1Volume v1Volume : volumes) {
            if (v1Volume.getName().equals(volumeMounts.getName())) {
                volume = v1Volume;
                if (volume.getPersistentVolumeClaim() != null) {
                    sourceType = "PersistentVolumeClaim";
                    sourceName = volume.getPersistentVolumeClaim().getClaimName();
                } else if (volume.getConfigMap() != null) {
                    sourceType = "ConfigMap";
                    sourceName = volume.getConfigMap().getName();
                } else if (volume.getProjected() != null) {
                    sourceType = "Projected";
                } else if (volume.getHostPath() != null) {
                    sourceType = "HostPath";
                    sourceName = volume.getHostPath().getPath();
                } else if (volume.getSecret() != null) {
                    sourceType = "Secret";
                    sourceName = volume.getSecret().getSecretName();
                } else if (volume.getAzureDisk() != null) {
                    sourceType = "AzureDisk";
                    sourceName = volume.getAzureDisk().getDiskName();
                } else if (volume.getAzureFile() != null) {
                    sourceType = "AzureFile";
                    sourceName = volume.getAzureFile().getShareName();
                } else if (volume.getCephfs() != null) {
                    sourceType = "Cephfs";
                    sourceName = volume.getCephfs().getMonitors().toString();
                } else if (volume.getCinder() != null) {
                    sourceType = "Cinder";
                    sourceName = volume.getCinder().getVolumeID();
                } else if (volume.getDownwardAPI() != null) {
                    sourceType = "DownwardAPI";
                } else if (volume.getFc() != null) {
                    sourceType = "FC";
                    sourceName = volume.getFc().getFsType();
                } else if (volume.getFlexVolume() != null) {
                    sourceType = "FlexVolume";
                    sourceName = volume.getFlexVolume().getDriver();
                } else if (volume.getFlocker() != null) {
                    sourceType = "Flocker";
                    sourceName = volume.getFlocker().getDatasetName();
                } else if (volume.getGcePersistentDisk() != null) {
                    sourceType = "GCEPersistentDisk";
                    sourceName = volume.getGcePersistentDisk().getPdName();
                } else if (volume.getGlusterfs() != null) {
                    sourceType = "Glusterfs";
                    sourceName = volume.getGlusterfs().getEndpoints();
                } else if (volume.getIscsi() != null) {
                    sourceType = "Iscsi";
                    sourceName = volume.getIscsi().getTargetPortal();
                } else if (volume.getNfs() != null) {
                    sourceType = "NFS";
                    sourceName = volume.getNfs().getServer();
                } else if (volume.getPersistentVolumeClaim() != null) {
                    sourceType = "PersistentVolumeClaim";
                    sourceName = volume.getPersistentVolumeClaim().getClaimName();
                } else if (volume.getPhotonPersistentDisk() != null) {
                    sourceType = "PhotonPersistentDisk";
                    sourceName = volume.getPhotonPersistentDisk().getPdID();
                } else if (volume.getEmptyDir() != null) {
                    sourceType = "EmptyDir";
                } else if (volume.getPortworxVolume() != null) {
                    sourceType = "PortworxVolume";
                    sourceName = volume.getPortworxVolume().getVolumeID();
                }
            }
        }
        return MountDto.builder()
            .name(volumeMounts.getName())
            .readOnly(volumeMounts.getReadOnly() != null && volumeMounts.getReadOnly())
            .mountPath(volumeMounts.getMountPath())
            .subPath(volumeMounts.getSubPath())
            .sourceType(sourceType)
            .sourceName(sourceName)
            .build();
    }
}
