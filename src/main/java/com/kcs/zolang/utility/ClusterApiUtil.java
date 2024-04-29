package com.kcs.zolang.utility;

import com.kcs.zolang.config.SSLConfig;
import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.domain.User;
import com.kcs.zolang.dto.request.RegisterClusterDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Component
@RequiredArgsConstructor
public class ClusterApiUtil {
    // private final SSLConfig sslConfigurator;

    // public RestTemplate createRestTemplateForCluster(Cluster cluster) throws Exception {
    //     SSLContext sslContext = createSSLContext(cluster.getCertPath());
    //     CloseableHttpClient httpClient = HttpClients.custom()
    //             .setConnectionManager(
    //                     PoolingHttpClientConnectionManagerBuilder.create()
    //                             .setSSLSocketFactory(
    //                                     SSLConnectionSocketFactoryBuilder.create()
    //                                             .setSslContext(sslContext)
    //                                             .build())
    //                             .build())
    //             .build();
    //     HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
    //     return new RestTemplate(requestFactory);
    // }

    // public Cluster registerNewCluster(User user, RegisterClusterDto clusterDto, InputStream certificateStream, String filename) throws Exception {
    //     String certPath = sslConfigurator.storeCertificate(certificateStream, filename, user.getId(), clusterDto.clusterName());
    //     return Cluster.builder()
    //             .clusterName(clusterDto.clusterName())
    //             .domainUrl(clusterDto.domainUrl())
    //             .secretToken(clusterDto.secretToken())
    //             .user(user)
    //             .version(clusterDto.version())
    //             .certPath(certPath)
    //             .build();
    // }
    // public SSLContext createSSLContext(String certPath) throws Exception {
    //     try {
    //         log.info("Certificate file path : {}", certPath);

    //         // PEM 형식의 파일을 읽기
    //         FileReader fileReader = new FileReader(certPath);
    //         PemReader pemReader = new PemReader(fileReader);
    //         ByteArrayInputStream inputStream = new ByteArrayInputStream(pemReader.readPemObject().getContent());

    //         // CertificateFactory를 사용하여 X.509 형식의 인증서 생성
    //         CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    //         X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);
    //         pemReader.close();

    //         // KeyStore에 인증서 추가
    //         KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    //         keyStore.load(null, null);
    //         keyStore.setCertificateEntry("certificate", certificate);

    //         TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    //         tmf.init(keyStore);

    //         SSLContext sslContext = SSLContext.getInstance("TLS");
    //         sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
    //         return sslContext;
    //     } catch (Exception e) {
    //         log.error("Failed to load certificate file", e);
    //         throw e;
    //     }
    // }

    public boolean getClusterStatus(String url, String token, RestTemplate restTemplate) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .path("/api/v1/nodes");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                builder.build().encode().toUri(), HttpMethod.GET, entity, Map.class);

        if(response.getStatusCode() == HttpStatus.OK && response.hasBody()) {
            Map<String,Object> body = response.getBody();
            return checkNodesStatus(body);
        }
        return false;
    }

    public List<Map<String,Object>> getClusterNodes(String url, String token, RestTemplate restTemplate) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .path("/api/v1/nodes");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                builder.build().encode().toUri(), HttpMethod.GET, entity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.hasBody()) {
            Map<String, Object> body = response.getBody();
            return extractNodesDetails(body);
        }
        return List.of();
    }

    private Boolean checkNodesStatus(Map<String,Object> nodesInfo) {
        if (nodesInfo != null && nodesInfo.containsKey("items")) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) nodesInfo.get("items");
            for (Map<String, Object> item : items) {
                Map<String, Object> status = (Map<String, Object>) item.get("status");
                Map<String, Object> conditions = ((List<Map<String, Object>>) status.get("conditions")).get(0);
                if (!"True".equals(conditions.get("status"))) {
                    return false; // 하나라도 비정상 상태면 false 반환
                }
            }
        }
        return true;
    }
    private List<Map<String, Object>> extractNodesDetails(Map<String, Object> nodesInfo) {
        List<Map<String, Object>> result = new ArrayList<>();  // 빈 리스트 생성 대신 ArrayList 인스턴스를 생성
        if (nodesInfo != null && nodesInfo.containsKey("items")) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) nodesInfo.get("items");
            for (Map<String, Object> item : items) {
                Map<String, Object> nodeDetails = new HashMap<>();  // 각 노드의 상세 정보를 저장할 Map 생성
                Map<String, Object> metadata = (Map<String, Object>) item.get("metadata");
                Map<String, Object> status = (Map<String, Object>) item.get("status");
                Map<String, Object> nodeInfo = (Map<String, Object>) status.get("nodeInfo");

                nodeDetails.put("created", metadata.get("creationTimestamp"));
                nodeDetails.put("name", metadata.get("name"));
                nodeDetails.put("addresses", status.get("addresses"));
                nodeDetails.put("capacity", status.get("capacity"));
                nodeDetails.put("allocatable", status.get("allocatable"));
                nodeDetails.put("conditions", status.get("conditions"));
                nodeDetails.put("OS", nodeInfo.get("operatingSystem"));
                nodeDetails.put("OSImage", nodeInfo.get("osImage"));
                nodeDetails.put("kernelVersion", nodeInfo.get("kernelVersion"));
                nodeDetails.put("containerRuntime", nodeInfo.get("containerRuntimeVersion"));
                nodeDetails.put("KubeletVersion", nodeInfo.get("kubeletVersion"));

                result.add(nodeDetails);  // 개별 노드의 상세 정보를 결과 리스트에 추가
            }
        }
        return result;  // 모든 노드의 상세 정보가 담긴 리스트 반환
    }
}
