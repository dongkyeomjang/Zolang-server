package com.kcs.zolang.utility;

import static io.kubernetes.client.extended.kubectl.Kubectl.top;

import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.domain.User;
import com.kcs.zolang.dto.response.UsageDto;
import com.kcs.zolang.dto.response.UserUrlTokenDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.ClusterRepository;
import com.kcs.zolang.repository.UserRepository;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringUtil {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy. MM. dd. a HH:mm:ss");
    private final ClusterRepository clusterRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public static String getAge(LocalDateTime past) {
        if (past == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();

        long years = ChronoUnit.YEARS.between(past, now);
        if (years > 0) {
            return years + " year";
        }

        long months = ChronoUnit.MONTHS.between(past, now);
        if (months > 0) {
            return months + " month";
        }

        long days = ChronoUnit.DAYS.between(past, now);
        if (days > 0) {
            return days + " day";
        }

        long hours = ChronoUnit.HOURS.between(past, now);
        if (hours > 0) {
            return hours + " hour";
        }

        long minutes = ChronoUnit.MINUTES.between(past, now);
        return minutes + " min";
    }

    public static String byteConverter(String num) {
        long bytes = Long.parseLong(num);
        final String[] units = new String[]{"B", "Ki", "Mi", "Gi", "Ti"};
        final DecimalFormat decimalFormat = new DecimalFormat("#.##");
        if (bytes <= 0) {
            return "0 B";
        }
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return decimalFormat.format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public ApiClient getV1Api(Long userId, Long clusterId) {
        Cluster clusters = clusterRepository.findById(clusterId)
            .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_CLUSTER));
        if (!clusters.getUser().getId().equals(userId)) {
            throw new CommonException(ErrorCode.NOT_FOUND_CLUSTER);
        }
        UserUrlTokenDto userUrlTokenDto = UserUrlTokenDto.fromEntity(clusters);
        ApiClient client = Config.fromToken("https://" + userUrlTokenDto.url(),
            userUrlTokenDto.token(), false);
        Configuration.setDefaultApiClient(client);
        return client;
    }

    //매분마다
    @Scheduled(cron = "0 * * * * *")
    public void saveResourceUsage() throws ApiException, KubectlException {
        List<User> users = userRepository.findAll();
        int h = LocalDateTime.now().getHour();
        int m = LocalDateTime.now().getMinute();
        String time = h + ":" + (m < 10 ? "0" + m : m);
        for (User user : users) {
            List<Cluster> clusters = clusterRepository.findByUserId(user.getId());
            for (Cluster cluster : clusters) {
                Map<String, Double> namespaceCpuUsage = new HashMap<>();
                Map<String, Long> namespaceMemoryUsage = new HashMap<>();
                double totalCpuUsage = 0;
                long totalMemoryUsage = 0;
                ApiClient client = getV1Api(user.getId(), cluster.getId());
                CoreV1Api coreV1Api = new CoreV1Api();
                V1PodList podList = coreV1Api.listPodForAllNamespaces().execute();
                for (V1Pod pod : podList.getItems()) {
                    String name = pod.getMetadata().getName();
                    String namespace = pod.getMetadata().getNamespace();
                    PodMetrics usage = top(V1Pod.class, PodMetrics.class).apiClient(client)
                        .name(name).namespace(namespace).execute().get(0).getRight();
                    if (usage == null) {
                        continue;
                    }
                    UsageDto podUsage = UsageDto.fromEntity(usage, time);
                    totalCpuUsage += podUsage.cpuUsage();
                    totalMemoryUsage += podUsage.memoryUsage();
                    if (namespaceCpuUsage.containsKey(namespace)) {
                        namespaceCpuUsage.put(namespace,
                            namespaceCpuUsage.get(namespace) + podUsage.cpuUsage());
                        namespaceMemoryUsage.put(namespace,
                            namespaceMemoryUsage.get(namespace) + podUsage.memoryUsage());
                    } else {
                        namespaceCpuUsage.put(namespace, podUsage.cpuUsage());
                        namespaceMemoryUsage.put(namespace, podUsage.memoryUsage());
                    }
                    redisTemplate.opsForValue()
                        .set("cluster-usage:" + cluster.getId() + ":" + name + ":" + m,
                            podUsage);
                    redisTemplate.expire("cluster-usage:" + cluster.getId() + ":" + name + ":" + m,
                        15, TimeUnit.MINUTES);
                }
                UsageDto totalUsage = UsageDto.fromEntity(totalCpuUsage, totalMemoryUsage, time);
                redisTemplate.opsForValue()
                    .set("cluster-usage:" + cluster.getId() + ":totalCpuUsage:" + m,
                        totalUsage);
                redisTemplate.expire("cluster-usage:" + cluster.getId() + ":totalCpuUsage:" + m,
                    15, TimeUnit.MINUTES);
                for (String namespace : namespaceCpuUsage.keySet()) {
                    UsageDto namespaceUsage = UsageDto.fromEntity(namespaceCpuUsage.get(namespace),
                        namespaceMemoryUsage.get(namespace), time);
                    redisTemplate.opsForValue()
                        .set("cluster-usage:" + cluster.getId() + ":" + namespace + ":" + m,
                            namespaceUsage);
                    redisTemplate.expire(
                        "cluster-usage:" + cluster.getId() + ":" + namespace + ":" + m, 15,
                        TimeUnit.MINUTES);
                }
            }
        }
        log.info("Resource Usage 저장 완료");
    }
}
