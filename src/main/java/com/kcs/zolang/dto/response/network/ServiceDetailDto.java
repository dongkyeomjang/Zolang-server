package com.kcs.zolang.dto.response.network;

import static com.kcs.zolang.utility.MonitoringUtil.getAge;

import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.openapi.models.V1LoadBalancerIngress;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;

@Builder
@Schema(name = "ServiceDetailDto", description = "유저 Service Detail 정보 Dto")
public record ServiceDetailDto(
        @Schema(description = "MetaData") MetaData metaData,
        @Schema(description = "Spec") Spec spec,
        @Schema(description = "Status") Status status) {

    public static ServiceDetailDto fromEntity(V1Service service) {
        MetaData metaData = new MetaData(
                service.getMetadata().getCreationTimestamp().toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                getAge(Objects.requireNonNull(service.getMetadata().getCreationTimestamp())
                        .toLocalDateTime()),
                service.getMetadata().getName(),
                service.getMetadata().getNamespace(),
                service.getMetadata().getLabels(),
                service.getMetadata().getAnnotations()
        );

        Spec spec = new Spec(
                service.getSpec().getPorts().stream().map(V1ServicePort::getPort)
                        .collect(Collectors.toList()),
                service.getSpec().getSelector(),
                service.getSpec().getType(),
                service.getSpec().getClusterIP(),
                service.getSpec().getIpFamilies(),
                service.getSpec().getIpFamilyPolicy()
        );

        //ingress null처리
        List<String> loadBalancerIngress = service.getStatus().getLoadBalancer() != null && service.getStatus().getLoadBalancer().getIngress() != null
                ? service.getStatus().getLoadBalancer().getIngress().stream()
                .map(V1LoadBalancerIngress::getHostname)
                .collect(Collectors.toList())
                : null;

        Status status = new Status(
                loadBalancerIngress
        );

        return new ServiceDetailDto(metaData, spec, status);
    }

    @Schema(name = "MetaData", description = "Service MetaData")
    public record MetaData(
            @Schema(description = "Network(service) 생성일", example = "2024-03-20T23:26:36+9:00") String serviceTimeStamp,
            @Schema(description = "Network(service) 생성 시간", example = "5 day") String serviceAge,
            @Schema(description = "Network(service) 이름", example = "kubernetes") String serviceName,
            @Schema(description = "Network(service) 네임스페이스", example = "default") String serviceNamespace,
            @Schema(description = "Network(service) 레이블", example = "app: cert-manager,") Map<String, String> serviceLabels,
            @Schema(description = "Network(service) 어노테이션", example = "kubectl.kubernetes.io/last-applied-configuratio : {apiversion1}") Map<String, String> serviceAnnotations) {

    }

    @Schema(name = "Spec", description = "Service Spec")
    public record Spec(
            @Schema(description = "Network(service) 포트", example = "[9402]") List<Integer> servicePort,
            @Schema(description = "Network(service) 셀렉터", example = "{app./compo: controller,..}") Map<String, String> serviceSelector,
            @Schema(description = "Network(service) 타입", example = "ClusterIP") String serviceType,
            @Schema(description = "Network(service) 클러스터 IP", example = "10.100.158.205") String serviceClusterIp,
            @Schema(description = "Network(service) IP 패밀리", example = "[Ipv4]") List<String> serviceIpFamiles,
            @Schema(description = "Network(service) IP 패밀리 정책", example = "SingleStack") String serviceIpFamilyPolicy) {

    }

    @Schema(name = "Status", description = "Service Status")
    public record Status(
            @Schema(description = "LoadBalancer Ingress 호스트네임", example = "[\"192.168.1.1\"]") List<String> loadBalancerIngress) {

    }
}
