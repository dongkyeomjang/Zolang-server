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
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    @Scheduled(cron = "0 */1 * * * *")
    public void saveResourceUsage() throws ApiException, KubectlException {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            List<Cluster> clusters = clusterRepository.findByUserId(user.getId());
            for (Cluster cluster : clusters) {
                ApiClient client = getV1Api(user.getId(), cluster.getId());
                CoreV1Api coreV1Api = new CoreV1Api();
                V1PodList podList = coreV1Api.listPodForAllNamespaces().execute();
                for (V1Pod pod : podList.getItems()) {
                    String name = pod.getMetadata().getName();
                    String namespace = pod.getMetadata().getNamespace();
                    UsageDto podUsage = UsageDto.fromEntity(
                        top(V1Pod.class, PodMetrics.class).apiClient(client)
                            .name(name).namespace(namespace).execute().get(0).getRight());
                    // 클러스터 cpu 사용량과 메모리 사용량을 레디스에 저장하기
                    int m = LocalDateTime.now().getMinute();
                    redisTemplate.opsForValue()
                        .set("cluster-usage:" + cluster.getId() + ":" + name + ":" + m,
                            podUsage);
                    redisTemplate.expire("cluster-usage:" + cluster.getId() + ":" + name + ":" + m,
                        15, TimeUnit.MINUTES);
                }
            }
        }
        System.out.println("Resource Usage 저장 완료");
    }
}
