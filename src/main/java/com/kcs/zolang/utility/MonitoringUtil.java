package com.kcs.zolang.utility;

import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.dto.response.UserUrlTokenDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.ClusterRepository;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonitoringUtil {

    private final ClusterRepository clusterRepository;

    public static String getAge(LocalDateTime created) {
        LocalDateTime now = LocalDateTime.now();
        String extension;
        int age;
        int passedHour = now.getMinute() > created.getMinute() ? 0 : 1;
        //1시간 이상 지난 경우 0, 아니면 1
        int passedDay = now.getHour() - passedHour > created.getHour() ? 0 : 1;
        //1년 이상 지난 경우 0, 아니면 1
        int passedYear = now.getDayOfYear() - passedDay > created.getDayOfYear() ? 0 : 1;
        age = now.getYear() - created.getYear() - passedYear;
        if (age < 1) {
            age = (365 + (now.getDayOfYear() - created.getDayOfYear() - passedDay)) % 365;
            if (age < 1) {
                age = now.getHour() - created.getHour() - passedHour;
                if (age < 1) {
                    age = now.getMinute() - created.getMinute();
                    extension = "m";
                } else {
                    extension = "h";
                }
            } else {
                extension = "d";
            }
        } else {
            extension = "y";
        }
        return age + extension;
    }

    public void getV1Api(Long userId) {
        List<Cluster> clusters = clusterRepository.findByUserId(userId);
        if (clusters.isEmpty()) {
            throw new CommonException(ErrorCode.NOT_FOUND_CLUSTER);
        }
        UserUrlTokenDto userUrlTokenDto = UserUrlTokenDto.fromEntity(clusters.get(0));
        //false: ignore cert
        ApiClient client = Config.fromToken("https://" + userUrlTokenDto.url(),
            userUrlTokenDto.token(), false);
        //TODO: SSL 인증서 추가
        /*InputStream caCertInputStream = new ByteArrayInputStream(userUrlTokenDto.caCert().getBytes(StandardCharsets.UTF_8));
        client.setSslCaCert(caCertInputStream);*/
        Configuration.setDefaultApiClient(client);
    }
}
