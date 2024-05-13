package com.kcs.zolang.utility;

import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.dto.response.UserUrlTokenDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.ClusterRepository;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonitoringUtil {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy. MM. dd. a HH:mm:ss");
    private final ClusterRepository clusterRepository;

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
        if (clusters.getUser().getId() != userId) {
            throw new CommonException(ErrorCode.NOT_FOUND_CLUSTER);
        }
        UserUrlTokenDto userUrlTokenDto = UserUrlTokenDto.fromEntity(clusters);
        ApiClient client = Config.fromToken("https://" + userUrlTokenDto.url(),
            userUrlTokenDto.token(), false);
        Configuration.setDefaultApiClient(client);
        return client;
    }
}
