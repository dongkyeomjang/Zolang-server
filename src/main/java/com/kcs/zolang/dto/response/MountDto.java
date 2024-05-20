package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "MountDto", description = "마운트 정보 Dto")
public record MountDto(
    String name,
    boolean readOnly,
    String mountPath,
    String subPath,
    String sourceType,
    String sourceName
) {

    public static MountDto fromEntity(V1VolumeMount volumeMounts, List<V1Volume> volumes) {
        V1Volume volume;
        String sourceType = null;
        String sourceName = null;
        for (int i = 0; i < volumes.size(); i++) {
            if (volumes.get(i).getName().equals(volumeMounts.getName())) {
                volume = volumes.get(i);
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
                    sourceName = volume.getFc().getTargetWWNs().toString();
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
