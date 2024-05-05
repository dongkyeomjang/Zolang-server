package com.kcs.zolang.dto.response;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Affinity;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodCondition;
import io.kubernetes.client.openapi.models.V1PodIP;
import io.kubernetes.client.openapi.models.V1Toleration;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record PodDto(
    @Schema(description = "Pod UID", example = "pod-uid")
    String uid,
    @Schema(description = "Pod 이름", example = "pod-name")
    String name,
    @Schema(description = "Conditions", example = "ready")
    List<String> conditions,
    @Schema(description = "Pod 네임스페이스", example = "default")
    String namespace,
    @Schema(description = "Pod CPU", example = "0.5")
    @Nullable
    Double cpu,
    @Schema(description = "Pod 메모리", example = "512")
    @Nullable
    Long memory,
    @Schema(description = "Pod 재시작 횟수", example = "0")
    List<Integer> restartCount,
    @Schema(description = "Pod QoS", example = "Guaranteed")
    String qos,
    @Schema(description = "Pod 노드", example = "node-name")
    String node,
    @Schema(description = "Pod 생성 시간", example = "1d")
    String age,
    @Schema(description = "Pod 상태", example = "Running")
    String status,
    @Schema(description = "Pod 제어자", example = "Deployment")
    List<String> controlledBy,
    @Schema(description = "Pod 라벨", example = "k8ss-app:kube-dns")
    Map<String, String> labels,
    @Schema(description = "Pod IP", example = "12.3.4.123")
    String podIp,
    @Schema(description = "Pod IPs", example = "12.3.4.123")
    List<String> podIps,
    @Schema(description = "Service Account", example = "default")
    String serviceAccount,
    @Schema(description = "Priority Class", example = "default")
    String priorityClass,
    @Schema(description = "Node Selector", example = "app:nginx")
    Map<String, String> nodeSelector,
    @Schema(description = "Tolerations", example = "key:NoSchedule") //TODO: 자세히 알아보기
    List<V1Toleration> tolerations,
    @Schema(description = "Affinities", example = "podAffinity")//TODO: 자세히 알아보기
    V1Affinity affinities
){
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
        Quantity cpuLimit = pod.getSpec().getContainers().get(0).getResources().getLimits().get("cpu");
        Quantity memoryLimit = pod.getSpec().getContainers().get(0).getResources().getLimits().get("memory");
        return PodDto.builder()
            .name(pod.getMetadata().getName())
            .namespace(pod.getMetadata().getNamespace())
            .uid(pod.getMetadata().getUid())
            .conditions(pod.getStatus().getConditions().stream().map(V1PodCondition::getType).toList())
            .cpu(cpuLimit!=null?cpuLimit.getNumber().doubleValue():null)
            .memory(memoryLimit!=null?memoryLimit.getNumber().longValue():null)
            .restartCount(pod.getStatus().getContainerStatuses().stream().map(V1ContainerStatus::getRestartCount).toList())
            .qos(pod.getStatus().getQosClass())
            .node(pod.getSpec().getNodeName())
            .age(age)
            .status(pod.getStatus().getPhase())
            .controlledBy(pod.getMetadata().getOwnerReferences().stream().map(V1OwnerReference::getKind).toList())
                .labels(pod.getMetadata().getLabels())
                .podIp(pod.getStatus().getPodIP())
                .podIps(pod.getStatus().getPodIPs().stream().map(V1PodIP::getIp).toList())
                .serviceAccount(pod.getSpec().getServiceAccountName())
                .priorityClass(pod.getSpec().getPriorityClassName())
                .nodeSelector(pod.getSpec().getNodeSelector())
                .tolerations(pod.getSpec().getTolerations())
                .affinities(pod.getSpec().getAffinity())
                .build();
    }
}
