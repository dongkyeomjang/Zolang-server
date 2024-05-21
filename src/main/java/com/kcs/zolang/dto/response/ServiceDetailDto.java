package com.kcs.zolang.dto.response;

import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.kcs.zolang.utility.MonitoringUtil.getAge;

@Getter
@Builder
@Schema(name = "ServiceDetailDto", description = "유저 Service Detail 정보 Dto")
public class ServiceDetailDto {

        @Schema(description = "MetaData")
        private final MetaData metaData;

        @Schema(description = "Spec")
        private final Spec spec;

        @Schema(description = "Status")
        private final Status status;

        @Getter
        @Builder
        @Schema(name = "MetaData", description = "Service MetaData")
        public static class MetaData {
                @Schema(description = "Network(service) 생성일", example = "2024-03-20T23:26:36+9:00")
                private final String serviceTimeStamp;
                @Schema(description = "Network(service) 생성 시간", example = "5 day")
                private final String serviceAge;
                @Schema(description = "Network(service) 이름", example = "kubernetes")
                private final String serviceName;
                @Schema(description = "Network(service) 네임스페이스", example = "default")
                private final String serviceNamespace;
                @Schema(description = "Network(service) 레이블", example = "app: cert-manager,")
                private final Map<String, String> serviceLabels;
                @Schema(description = "Network(service) 어노테이션", example = "kubectl.kubernetes.io/last-applied-configuratio : {apiversion1}")
                private final Map<String, String> serviceAnnotations;
        }

        @Getter
        @Builder
        @Schema(name = "Spec", description = "Service Spec")
        public static class Spec {
                @Schema(description = "Network(service) 포트", example = "[9402]")
                private final List<Integer> servicePort;
                @Schema(description = "Network(service) 셀렉터", example = "{app./compo: controller,..}")
                private final Map<String, String> serviceSelector;
                @Schema(description = "Network(service) 타입",example = "ClusterIP")
                private final String serviceType;
                @Schema(description = "Network(service) 클러스터 IP", example = "10.100.158.205")
                private final String serviceClusterIp;
                @Schema(description = "Network(service) IP 패밀리", example = "[Ipv4]")
                private final List<String> serviceIpFamiles;
                @Schema(description = "Network(service) IP 패밀리 정책", example = "SingleStack")
                private final String serviceIpFamilyPolicy;
        }

        @Getter
        @Builder
        @Schema(name = "Status", description = "Service Status")
        public static class Status {
                @Schema(description = "Network(service) 상태")
                private final Map<String, String> serviceStatus;
        }

        public static ServiceDetailDto fromEntity(V1Service service) {
                MetaData metaData = MetaData.builder()
                        .serviceTimeStamp(service.getMetadata().getCreationTimestamp().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .serviceAge(getAge(
                                Objects.requireNonNull(service.getMetadata().getCreationTimestamp()).toLocalDateTime()))
                        .serviceName(service.getMetadata().getName())
                        .serviceNamespace(service.getMetadata().getNamespace())
                        .serviceLabels(service.getMetadata().getLabels())
                        .serviceAnnotations(service.getMetadata().getAnnotations())
                        .build();

                Spec spec = Spec.builder()
                        .servicePort(service.getSpec().getPorts().stream().map(V1ServicePort::getPort).collect(Collectors.toList()))
                        .serviceSelector(service.getSpec().getSelector())
                        .serviceType(service.getSpec().getType())
                        .serviceClusterIp(service.getSpec().getClusterIP())
                        .serviceIpFamiles(service.getSpec().getIpFamilies())
                        .serviceIpFamilyPolicy(service.getSpec().getIpFamilyPolicy())
                        .build();

                Status status = Status.builder()
                        //.serviceStatus(service.getStatus() != null ? service.getStatus().getLoadBalancer().getIngress() : null)
                        .build();

                return ServiceDetailDto.builder()
                        .metaData(metaData)
                        .spec(spec)
                        .status(status)
                        .build();
        }
}
