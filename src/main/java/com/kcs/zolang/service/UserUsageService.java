package com.kcs.zolang.service;

import com.kcs.zolang.domain.Bill;
import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.domain.Usage;
import com.kcs.zolang.domain.User;
import com.kcs.zolang.dto.response.cluster.UserUsageBillDto;
import com.kcs.zolang.dto.response.cluster.UserUsageDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.BillRepository;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserUsageService {

    private final ClusterRepository clusterRepository;
    private final MonitoringUtil monitoringUtil;
    private final UsageRepository usageRepository;
    private final UserRepository userRepository;
    private final BillRepository billRepository;

    //모든 클러스터 총 합(실시간)
    public UserUsageDto getUserUsage(Long userId) {
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
                        .filter(metric -> metric.getMetadata().getName()
                            .equals(node.getMetadata().getName()))
                        .findFirst()
                        .orElse(null);

                    if (nodeMetrics != null) {
                        totalCpuUsage += Double.parseDouble(
                            nodeMetrics.getUsage().get("cpu").getNumber().toString());
                        totalMemoryUsage += nodeMetrics.getUsage().get("memory").getNumber()
                            .longValue();
                    }

                    totalCpuAllocatable += Double.parseDouble(
                        node.getStatus().getAllocatable().get("cpu").getNumber().toString());
                    totalMemoryAllocatable += node.getStatus().getAllocatable().get("memory")
                        .getNumber().longValue();
                    totalPodAllocatable += Integer.parseInt(
                        node.getStatus().getAllocatable().get("pods").getNumber().toString());

                    totalCpuCapacity += Double.parseDouble(
                        node.getStatus().getCapacity().get("cpu").getNumber().toString());
                    totalMemoryCapacity += node.getStatus().getCapacity().get("memory").getNumber()
                        .longValue();
                    totalPodCapacity += Integer.parseInt(
                        node.getStatus().getCapacity().get("pods").getNumber().toString());
                }

                totalPodUsage += (int) coreV1Api.listPodForAllNamespaces().execute().getItems()
                    .stream()
                    .filter(pod -> "Running".equals(pod.getStatus().getPhase()))
                    .count();

            } catch (ApiException e) {
                throw new CommonException(ErrorCode.API_ERROR);
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

    //usage 데이터 저장
    @Transactional
    @Scheduled(cron = "0 0 * * * *") // 1시간마다 데이터 저장
    public void saveHourlyUserUsage() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        if (usageRepository.existsByCreatedAtBetween(now, now.plusMinutes(50))) {
            return;
        }
        List<User> users = userRepository.findAll();
        for (User user : users) {
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
        }
    }

    //5일전 데이터 삭제
    @Transactional
    @Scheduled(cron = "0 0 0 * * *") // 정각마다 5일전 데이터 삭제
    public void deleteOldUsages() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(5);
        LocalDate thresholdLocalDate = thresholdDate.toLocalDate();
        String thresholdDateString = thresholdLocalDate.toString();

        // Usage 데이터 삭제
        usageRepository.deleteByCreatedAtBefore(thresholdDate);

        // Bill 데이터 삭제
        List<Bill> oldBills = billRepository.findAllByDateBefore(thresholdDateString);
        billRepository.deleteAll(oldBills);
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *") // 정각마다 하루 전 cost 저장
    public void saveDailyBill() {
        LocalDateTime now = LocalDateTime.now();
        if (billRepository.existsByDate(String.valueOf(LocalDate.now().minusDays(1)))) {
            return;
        }
        LocalDateTime startOfDay = now.minusDays(1).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        List<User> users = userRepository.findAll();

        for (User user : users) {
            List<Usage> usages = usageRepository.findAllByUserIdAndCreatedAtBetween(user.getId(),
                startOfDay, endOfDay);

            double totalCpuUsage = usages.stream().mapToDouble(Usage::getCpuUsage).sum();
            double totalCpuCapacity = usages.stream().mapToDouble(Usage::getCpuCapacity).sum();
            double totalMemoryUsage = usages.stream().mapToLong(Usage::getMemoryUsage).sum();
            double totalMemoryCapacity = usages.stream().mapToLong(Usage::getMemoryCapacity).sum();
            double totalPodUsage = usages.stream().mapToInt(Usage::getPodUsage).sum();
            double totalPodCapacity = usages.stream().mapToInt(Usage::getPodCapacity).sum();

            double cpuCost = (totalCpuUsage / totalCpuCapacity) * 15000;
            double memoryCost = (totalMemoryUsage / totalMemoryCapacity) * 15000;
            double podCost = (totalPodUsage / totalPodCapacity) * 15000;

            // Calculate runtime cost
            double runtimeCost = 0;
            List<Cluster> clusters = clusterRepository.findByUserId(user.getId());
            for (Cluster cluster : clusters) {
                LocalDateTime clusterCreatedAt = cluster.getCreatedAt();
                long runtimeMinutes;

                if (clusterCreatedAt.isBefore(startOfDay)) {
                    // 클러스터 생성일이 하루 시작 전인 경우
                    runtimeMinutes = 1440; // 하루 1440분
                } else if (clusterCreatedAt.isAfter(startOfDay) && clusterCreatedAt.isBefore(
                    endOfDay)) {
                    // 클러스터 생성일이 하루 중인 경우
                    runtimeMinutes = Duration.between(clusterCreatedAt, endOfDay).toMinutes();
                } else {
                    // 클러스터 생성일이 하루 끝난 후인 경우
                    runtimeMinutes = 0;
                }

                runtimeCost += runtimeMinutes * 150;
            }

            Bill bill = Bill.builder()
                .user(user)
                .cpuCost(Double.isNaN(cpuCost) ? 0 : cpuCost)
                .memoryCost(Double.isNaN(memoryCost) ? 0 : memoryCost)
                .podCost(Double.isNaN(podCost) ? 0 : podCost)
                .runtimeCost(runtimeCost)
                .date(String.valueOf(LocalDate.now().minusDays(1)))
                .build();

            billRepository.save(bill);
        }
    }

    //1일전 데이터 뽑기 (그래프용)
    public UserUsageDto getUserUsageAverage(Long userId) {
        LocalDate date = LocalDate.now();
        LocalDateTime startOfDay = date.minusDays(1).atStartOfDay();
        LocalDateTime endOfDay = date.minusDays(1).atTime(LocalTime.MAX);

        // 전날의 데이터
        List<Usage> usages = usageRepository.findAllByUserIdAndCreatedAtBetween(userId, startOfDay,
            endOfDay);

        if (usages.isEmpty()) {
            // 전날 데이터 없는 경우 0으로
            return new UserUsageDto(
                0.0,
                0.0,
                0.0,
                0L,
                0L,
                0L,
                0,
                0,
                0
            );
        }

        //평균 계산
        double avgCpuUsage = usages.stream().mapToDouble(Usage::getCpuUsage).average().orElse(0);
        double avgCpuCapacity = usages.stream().mapToDouble(Usage::getCpuCapacity).average()
            .orElse(0);
        double avgCpuAllocatable = usages.stream().mapToDouble(Usage::getCpuAllo).average()
            .orElse(0);
        long avgMemoryUsage = (long) usages.stream().mapToLong(Usage::getMemoryUsage).average()
            .orElse(0);
        long avgMemoryCapacity = (long) usages.stream().mapToLong(Usage::getMemoryCapacity)
            .average().orElse(0);
        long avgMemoryAllocatable = (long) usages.stream().mapToLong(Usage::getMemoryAllo).average()
            .orElse(0);
        int avgPodUsage = (int) usages.stream().mapToInt(Usage::getPodUsage).average().orElse(0);
        int avgPodCapacity = (int) usages.stream().mapToInt(Usage::getPodCapacity).average()
            .orElse(0);
        int avgPodAllocatable = (int) usages.stream().mapToInt(Usage::getPodAllo).average()
            .orElse(0);

//        log.info("Avg CPU Usage: {}", avgCpuUsage);
//        log.info("Avg CPU Capacity: {}", avgCpuCapacity);
//        log.info("Avg CPU Allocatable: {}", avgCpuAllocatable);
//        log.info("Avg Memory Usage: {}", avgMemoryUsage);
//        log.info("Avg Memory Capacity: {}", avgMemoryCapacity);
//        log.info("Avg Memory Allocatable: {}", avgMemoryAllocatable);
//        log.info("Avg Pod Usage: {}", avgPodUsage);
//        log.info("Avg Pod Capacity: {}", avgPodCapacity);
//        log.info("Avg Pod Allocatable: {}", avgPodAllocatable);

        return UserUsageDto.builder()
                .cpuUsage(avgCpuUsage)
                .cpuAllocatable(avgCpuAllocatable)
                .cpuCapacity(avgCpuCapacity)
                .memoryUsage(avgMemoryUsage)
                .memoryAllocatable(avgMemoryAllocatable)
                .memoryCapacity(avgMemoryCapacity)
                .podUsage(avgPodUsage)
                .podAllocatable(avgPodAllocatable)
                .podCapacity(avgPodCapacity)
                .build();
    }

    //4일치 데이터 + 실시간 데이터 뽑기(Bill지 용)
    public List<UserUsageBillDto> getUserUsageBill(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<UserUsageBillDto> usageBills = new ArrayList<>();

        for (int i = 1; i <= 4; i++) {
            LocalDateTime endOfDay = now.minusDays(i).truncatedTo(ChronoUnit.DAYS).plusDays(1)
                .minusNanos(1);
            LocalDateTime startOfDay = now.minusDays(i).truncatedTo(ChronoUnit.DAYS);

            double totalCpuUsage = 0;
            long totalMemoryUsage = 0;
            int totalPodUsage = 0;
            long totalClusterRuntime = 0;

            List<Usage> usages = usageRepository.findAllByUserIdAndCreatedAtBetween(userId,
                startOfDay, endOfDay);

            for (Usage usage : usages) {
                totalCpuUsage += usage.getCpuUsage();
                totalMemoryUsage += usage.getMemoryUsage();
                totalPodUsage += usage.getPodUsage();
            }

            List<Cluster> clusters = clusterRepository.findByUserId(userId);
            for (Cluster cluster : clusters) {
                LocalDateTime clusterCreatedAt = cluster.getCreatedAt();

                if (clusterCreatedAt.isBefore(startOfDay)) {
                    totalClusterRuntime += 1440; // 하루 1440분
                } else if (clusterCreatedAt.isAfter(startOfDay) && clusterCreatedAt.isBefore(
                    endOfDay)) {
                    totalClusterRuntime += Duration.between(clusterCreatedAt, endOfDay).toMinutes();
                } else if (clusterCreatedAt.isEqual(startOfDay)) {
                    totalClusterRuntime += Duration.between(clusterCreatedAt, endOfDay).toMinutes();
                }

            }

            Bill bill = billRepository.findByUserIdAndDate(userId,
                startOfDay.toLocalDate().toString());

            UserUsageBillDto usageBill = UserUsageBillDto.builder()
                .date(startOfDay.toLocalDate().toString())
                .totalCpuUsage(totalCpuUsage)
                .totalCpuCost(bill != null ? bill.getCpuCost() : 0)
                .totalMemoryUsage(totalMemoryUsage)
                .totalMemoryCost(bill != null ? bill.getMemoryCost() : 0)
                .totalPodUsage(totalPodUsage)
                .totalPodCost(bill != null ? bill.getPodCost() : 0)
                .totalClusterRuntime(totalClusterRuntime)
                .totalClusterRuntimeCost(totalClusterRuntime * 150)
                .totalCost(
                    bill != null ? bill.getCpuCost() + bill.getMemoryCost() + bill.getPodCost() + (
                        totalClusterRuntime * 150) : 0)
                .build();

            usageBills.add(usageBill);
        }

        return usageBills;
    }

    public List<UserUsageBillDto> getRealTimeBill(Long userId) {
        List<UserUsageBillDto> usageBills = getUserUsageBill(userId);

        // 실시간 데이터
        UserUsageDto realTimeUsage = getUserUsage(userId);

        //분모 0일 경우 제외
        double cpuCapacity = realTimeUsage.cpuCapacity() != 0 ? realTimeUsage.cpuCapacity() : 1;
        double memoryCapacity =
            realTimeUsage.memoryCapacity() != 0 ? realTimeUsage.memoryCapacity() : 1;
        double podCapacity = realTimeUsage.podCapacity() != 0 ? realTimeUsage.podCapacity() : 1;

        double realTimeCpuCost = (realTimeUsage.cpuUsage() / cpuCapacity) * 15000;
        double realTimeMemoryCost = (realTimeUsage.memoryUsage() / memoryCapacity) * 15000;
        double realTimePodCost = (realTimeUsage.podUsage() / podCapacity) * 15000;

        // 클러스터 런타임 계산
        long totalClusterRuntime = 0;
        LocalDateTime now = LocalDateTime.now();
        List<Cluster> clusters = clusterRepository.findByUserId(userId);
        for (Cluster cluster : clusters) {
            LocalDateTime clusterCreatedAt = cluster.getCreatedAt();

            if (clusterCreatedAt.toLocalDate().isBefore(now.toLocalDate())) {
                totalClusterRuntime += Duration.between(now.toLocalDate().atStartOfDay(), now)
                    .toMinutes(); // 오늘 정각부터 현재 시간까지
            } else {
                totalClusterRuntime += Duration.between(clusterCreatedAt, now)
                    .toMinutes(); // 생성 시간부터 현재 시간까지
            }
        }

        double realTimeRuntimeCost = totalClusterRuntime * 80;

        double realTimeTotalCost =
            realTimeCpuCost + realTimeMemoryCost + realTimePodCost + realTimeRuntimeCost;

        UserUsageBillDto realTimeBill = UserUsageBillDto.builder()
            .date(LocalDate.now().toString())
            .totalCpuUsage(realTimeUsage.cpuUsage())
            .totalCpuCost(realTimeCpuCost)
            .totalMemoryUsage(realTimeUsage.memoryUsage())
            .totalMemoryCost(realTimeMemoryCost)
            .totalPodUsage(realTimeUsage.podUsage())
            .totalPodCost(realTimePodCost)
            .totalClusterRuntime(totalClusterRuntime)
            .totalClusterRuntimeCost(realTimeRuntimeCost)
            .totalCost(realTimeTotalCost)
            .build();

        usageBills.add(0, realTimeBill); // 실시간 데이터 맨 앞으로

        return usageBills;
    }


}
