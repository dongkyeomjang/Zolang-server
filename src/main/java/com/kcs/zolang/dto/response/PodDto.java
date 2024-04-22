package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1Pod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;

@Builder
@Schema(name = "PodDto", description = "유저 Pod 정보 Dto")
public record PodDto(
    String uid,
    String name,
    String namespace,
    Map<String, String> labels,
    String controlledBy,
    @Nullable
    Double cpu,
   @Nullable
    Long memory,
    int restartCount,
    String node,
    String qos,
    String age,
    String status
) implements Serializable {
    public static PodDto fromEntity(V1Pod pod) {
        LocalDateTime created = pod.getMetadata().getCreationTimestamp().toLocalDateTime();
        LocalDateTime now = LocalDateTime.now();
        String age;
            age = String.valueOf(now.getDayOfYear() - created.getDayOfYear());
            if (age.equals("0")) {
                age = String.valueOf(now.getHour() - created.getHour());
                if (age.equals("0")) {
                    age = now.getMinute() - created.getMinute()+"m";
                }else {
                    age = age + "h";
                }
            }else {
                age = age + "d";
        }
        return PodDto.builder()
            .name(pod.getMetadata().getName())
            .namespace(pod.getMetadata().getNamespace())
            .uid(pod.getMetadata().getUid())
            .labels(pod.getMetadata().getLabels())
            .cpu(pod.getSpec().getContainers().get(0).getResources()
                .getLimits().get("cpu")!=null?pod.getSpec().getContainers().get(0).getResources().getLimits().get("cpu").getNumber().doubleValue():null)
            .memory(pod.getSpec().getContainers().get(0).getResources().getLimits().get("memory")!=null?pod.getSpec().getContainers().get(0).getResources().getLimits().get("memory").getNumber().longValue():null)
            .restartCount(pod.getStatus().getContainerStatuses().get(0).getRestartCount())
            .qos(pod.getStatus().getQosClass())
            .node(pod.getSpec().getNodeName())
            .age(age)
            .status(pod.getStatus().getPhase())
            .controlledBy(pod.getMetadata().getOwnerReferences().get(0).getKind())
            .build();
    }
}
