package com.kcs.zolang.service;

import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.domain.Usage;
import com.kcs.zolang.domain.User;
import com.kcs.zolang.dto.response.cluster.UserUsageDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.ClusterRepository;
import com.kcs.zolang.repository.UsageRepository;
import com.kcs.zolang.repository.UserRepository;
import com.kcs.zolang.utility.MonitoringUtil;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.custom.NodeMetrics;
import io.kubernetes.client.custom.NodeMetricsList;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserUsageService {

    private final ClusterRepository clusterRepository;
    private final MonitoringUtil monitoringUtil;
    private final UsageRepository usageRepository;
    private final UserRepository userRepository;

    //모든 클러스터 총 합(실시간)
    public UserUsageDto getUserUsage(Long userId) throws Exception {
        List<Cluster> clusters = clusterRepository.findByUserId(userId).stream()
                .filter(cluster -> "zolang".equals(cluster.getProvider()))
                .collect(Collectors.toList());

        if (clusters.isEmpty()) {
            throw new CommonException(ErrorCode.NOT_FOUND_CLUSTER);
        }

        double totalCpuUsage = 0;
        double totalCpuAllocatable = 0;
        double totalCpuCapacity = 0;
        long totalMemoryUsage = 0;
        long totalMemoryAllocatable = 0;
        long totalMemoryCapacity = 0;
        int totalPodAllocatable = 0;
        int totalPodCapacity = 0;
        int totalPodUsage = 0;

        for (Cluster cluster : clusters) {
            ApiClient client = monitoringUtil.getV1Api(userId, cluster.getId());
            CoreV1Api coreV1Api = new CoreV1Api(client);
            Metrics metricsApi = new Metrics(client);

            try {
                V1NodeList nodeList = coreV1Api.listNode().execute();
                List<V1Node> nodes = nodeList.getItems();
                NodeMetricsList metricsList = metricsApi.getNodeMetrics();

                for (V1Node node : nodes) {
                    NodeMetrics nodeMetrics = metricsList.getItems().stream()
                            .filter(metric -> metric.getMetadata().getName().equals(node.getMetadata().getName()))
                            .findFirst()
                            .orElse(null);

                    if (nodeMetrics != null) {
                        totalCpuUsage += Double.parseDouble(nodeMetrics.getUsage().get("cpu").getNumber().toString());
                        totalMemoryUsage += nodeMetrics.getUsage().get("memory").getNumber().longValue();
                    }

                    totalCpuAllocatable += Double.parseDouble(node.getStatus().getAllocatable().get("cpu").getNumber().toString());
                    totalMemoryAllocatable += node.getStatus().getAllocatable().get("memory").getNumber().longValue();
                    totalPodAllocatable += Integer.parseInt(node.getStatus().getAllocatable().get("pods").getNumber().toString());

                    totalCpuCapacity += Double.parseDouble(node.getStatus().getCapacity().get("cpu").getNumber().toString());
                    totalMemoryCapacity += node.getStatus().getCapacity().get("memory").getNumber().longValue();
                    totalPodCapacity += Integer.parseInt(node.getStatus().getCapacity().get("pods").getNumber().toString());
                }

                totalPodUsage += (int) coreV1Api.listPodForAllNamespaces().execute().getItems().stream()
                        .filter(pod -> "Running".equals(pod.getStatus().getPhase()))
                        .count();

            } catch (ApiException e) {
                throw new RuntimeException("API error", e);
            }
        }

        return new UserUsageDto(
                totalCpuUsage,
                totalCpuAllocatable,
                totalCpuCapacity,
                totalMemoryUsage,
                totalMemoryAllocatable,
                totalMemoryCapacity,
                totalPodUsage,
                totalPodAllocatable,
                totalPodCapacity
        );
    }

    @Scheduled(cron = "0 0 * * * *") // 1시간마다 실행
    public void saveHourlyUserUsage() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                UserUsageDto userUsageDto = getUserUsage(user.getId());

                Usage usage = Usage.builder()
                        .user(user)
                        .cpuUsage(userUsageDto.cpuUsage())
                        .cpuCapacity(userUsageDto.cpuCapacity())
                        .cpuAllo(userUsageDto.cpuAllocatable())
                        .memoryUsage(userUsageDto.memoryUsage())
                        .memoryCapacity(userUsageDto.memoryCapacity())
                        .memoryAllo(userUsageDto.memoryAllocatable())
                        .podUsage(userUsageDto.podUsage())
                        .podCapacity(userUsageDto.podCapacity())
                        .podAllo(userUsageDto.podAllocatable())
                        .build();

                usageRepository.save(usage);
            } catch (Exception e) {
            }
        }
    }

    public UserUsageDto getUserUsageAverage(Long userId) {
        LocalDate date = LocalDate.now();
        LocalDateTime startOfDay = date.minusDays(1).atStartOfDay();
        LocalDateTime endOfDay = date.minusDays(1).atTime(LocalTime.MAX);

        List<Usage> usages = usageRepository.findAllByUserIdAndCreatedAtBetween(userId, startOfDay, endOfDay);


//        if (usages.isEmpty()) {
//            throw new CommonException(ErrorCode.NOT_FOUND_USAGE);
//        }

        double avgCpuUsage = usages.stream().mapToDouble(Usage::getCpuUsage).average().orElse(0);
        double avgCpuCapacity = usages.stream().mapToDouble(Usage::getCpuCapacity).average().orElse(0);
        double avgCpuAllocatable = usages.stream().mapToDouble(Usage::getCpuAllo).average().orElse(0);
        double avgMemoryUsage = usages.stream().mapToLong(Usage::getMemoryUsage).average().orElse(0);
        double avgMemoryCapacity = usages.stream().mapToLong(Usage::getMemoryCapacity).average().orElse(0);
        double avgMemoryAllocatable = usages.stream().mapToLong(Usage::getMemoryAllo).average().orElse(0);
        double avgPodUsage = usages.stream().mapToInt(Usage::getPodUsage).average().orElse(0);
        double avgPodCapacity = usages.stream().mapToInt(Usage::getPodCapacity).average().orElse(0);
        double avgPodAllocatable = usages.stream().mapToInt(Usage::getPodAllo).average().orElse(0);

        return new UserUsageDto(
                avgCpuUsage,
                avgCpuAllocatable,
                avgCpuCapacity,
                (long) avgMemoryUsage,
                (long) avgMemoryAllocatable,
                (long) avgMemoryCapacity,
                (int) avgPodUsage,
                (int) avgPodAllocatable,
                (int) avgPodCapacity
        );
    }
}
